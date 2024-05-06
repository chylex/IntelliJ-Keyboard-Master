package com.chylex.intellij.keyboardmaster.feature.vimNavigation

import com.chylex.intellij.keyboardmaster.PluginDisposableService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.toolWindow.InternalDecoratorImpl
import com.intellij.ui.SpeedSearchBase
import com.intellij.ui.speedSearch.SpeedSearch
import com.intellij.ui.speedSearch.SpeedSearchSupply
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.KeyStroke

internal open class VimNavigationDispatcher<T : JComponent>(final override val component: T, private val rootNode: KeyStrokeNode.Parent<VimNavigationDispatcher<T>>) : DumbAwareAction(), ComponentHolder {
	companion object {
		private val DISPOSABLE = ApplicationManager.getApplication().getService(PluginDisposableService::class.java)
		private val EXTRA_SHORTCUTS = setOf(
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
		)
		
		@Suppress("UnstableApiUsage")
		fun JComponent.getParentToolWindowId(): String? {
			return InternalDecoratorImpl.findNearestDecorator(this)?.toolWindowId
		}
	}
	
	private val originalEnterAction: ActionListener? = component.getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
	private var currentNode: KeyStrokeNode.Parent<VimNavigationDispatcher<T>> = rootNode
	var isSearching = AtomicBoolean(false)
	
	init {
		registerCustomShortcutSet(KeyStrokeNode.getAllShortcuts(getAllKeyStrokes()), component, DISPOSABLE)
		
		val speedSearch = SpeedSearchSupply.getSupply(component, true)
		speedSearch?.addChangeListener {
			if (it.propertyName == SpeedSearchSupply.ENTERED_PREFIX_PROPERTY_NAME && !speedSearch.isPopupActive) {
				isSearching.set(false)
			}
		}
	}
	
	protected fun getAllKeyStrokes(): Set<KeyStroke> {
		return KeyStrokeNode.getAllKeyStrokes(rootNode, EXTRA_SHORTCUTS)
	}
	
	final override fun actionPerformed(e: AnActionEvent) {
		val keyEvent = e.inputEvent as? KeyEvent ?: return
		
		if (keyEvent.id == KeyEvent.KEY_PRESSED && handleSpecialKeyPress(keyEvent)) {
			currentNode = rootNode
			return
		}
		
		when (val nextNode = currentNode.getChild(keyEvent)) {
			is KeyStrokeNode.Parent<VimNavigationDispatcher<T>>     -> currentNode = nextNode
			is KeyStrokeNode.ActionNode<VimNavigationDispatcher<T>> -> {
				nextNode.performAction(this, e, keyEvent)
				currentNode = rootNode
			}
		}
	}
	
	private fun handleSpecialKeyPress(keyEvent: KeyEvent): Boolean {
		if (keyEvent.keyCode == KeyEvent.VK_ESCAPE) {
			return true
		}
		
		if (keyEvent.keyCode == KeyEvent.VK_ENTER) {
			handleEnterKeyPress(ActionEvent(component, ActionEvent.ACTION_PERFORMED, "Enter", keyEvent.`when`, keyEvent.modifiersEx))
			return true
		}
		
		return false
	}
	
	private fun handleEnterKeyPress(e: ActionEvent) {
		if (isSearching.compareAndSet(true, false)) {
			when (val supply = SpeedSearchSupply.getSupply(component)) {
				is SpeedSearchBase<*> -> supply.hidePopup()
				is SpeedSearch        -> supply.reset()
			}
		}
		else {
			originalEnterAction?.actionPerformed(e)
		}
	}
	
	final override fun update(e: AnActionEvent) {
		e.presentation.isEnabled = !isSearching.get() || e.inputEvent.let { it is KeyEvent && it.id == KeyEvent.KEY_PRESSED && it.keyCode == KeyEvent.VK_ENTER }
	}
	
	final override fun getActionUpdateThread(): ActionUpdateThread {
		return ActionUpdateThread.BGT
	}
}
