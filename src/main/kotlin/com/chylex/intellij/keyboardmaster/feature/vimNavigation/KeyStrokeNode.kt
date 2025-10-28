package com.chylex.intellij.keyboardmaster.feature.vimNavigation

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.CustomizedDataContext
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.util.containers.map2Array
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

internal interface KeyStrokeNode<T> {
	class Parent<T>(private val keys: Map<KeyStroke, KeyStrokeNode<T>>) : KeyStrokeNode<T> {
		val allKeyStrokes: Set<KeyStroke> = mutableSetOf<KeyStroke>().apply {
			for ((key, node) in keys) {
				add(key)
				
				if (node is Parent) {
					addAll(node.allKeyStrokes)
				}
			}
		}
		
		fun getChild(keyEvent: KeyEvent): KeyStrokeNode<T> {
			val keyStroke = when {
				isCharEvent(keyEvent) -> KeyStroke.getKeyStroke(keyEvent.keyChar, keyEvent.modifiersEx and KeyEvent.SHIFT_DOWN_MASK.inv())
				isCodeEvent(keyEvent) -> KeyStroke.getKeyStroke(keyEvent.keyCode, keyEvent.modifiersEx, false)
				else                  -> return this
			}
			
			return keys[keyStroke] ?: this
		}
		
		private fun isCharEvent(keyEvent: KeyEvent): Boolean {
			return keyEvent.keyChar != KeyEvent.CHAR_UNDEFINED && (keyEvent.modifiersEx and KeyEvent.CTRL_DOWN_MASK) == 0
		}
		
		private fun isCodeEvent(keyEvent: KeyEvent): Boolean {
			return keyEvent.id == KeyEvent.KEY_PRESSED
		}
		
		operator fun plus(other: Parent<T>): Parent<T> {
			val mergedKeys = HashMap(keys)
			
			for ((otherKey, otherNode) in other.keys) {
				if (otherNode is Parent) {
					val ourNode = keys[otherKey]
					if (ourNode is Parent) {
						mergedKeys[otherKey] = ourNode + otherNode
						continue
					}
				}
				
				mergedKeys[otherKey] = otherNode
			}
			
			return Parent(mergedKeys)
		}
	}
	
	interface ActionNode<T> : KeyStrokeNode<T> {
		fun performAction(holder: T, actionEvent: AnActionEvent, keyEvent: KeyEvent)
	}
	
	class IdeaAction<T : ComponentHolder>(private val name: String) : ActionNode<T> {
		override fun performAction(holder: T, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val action = actionEvent.actionManager.getAction(name) ?: return
			
			val dataContext = CustomizedDataContext.withProvider(actionEvent.dataContext) {
				when {
					PlatformDataKeys.CONTEXT_COMPONENT.`is`(it) -> holder.component
					else                                        -> null
				}
			}
			
			ActionUtil.invokeAction(action, dataContext, actionEvent.place, null, null)
		}
	}
	
	companion object {
		fun <T> getAllKeyStrokes(root: Parent<T>, extra: Set<KeyStroke>? = null): Set<KeyStroke> {
			val allKeyStrokes = HashSet(root.allKeyStrokes)
			
			if (extra != null) {
				allKeyStrokes.addAll(extra)
			}
			
			for (c in ('a'..'z') + ('A'..'Z')) {
				allKeyStrokes.add(KeyStroke.getKeyStroke(c))
			}
			
			return allKeyStrokes
		}
		
		fun getAllShortcuts(keyStrokes: Set<KeyStroke>): CustomShortcutSet {
			return CustomShortcutSet(*keyStrokes.map2Array { KeyboardShortcut(it, null) })
		}
	}
}
