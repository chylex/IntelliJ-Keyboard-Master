package com.chylex.intellij.keyboardmaster.feature.vimNavigation

import com.chylex.intellij.keyboardmaster.PluginDisposableService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.toolWindow.InternalDecoratorImpl
import com.intellij.ui.SpeedSearchBase
import com.intellij.ui.speedSearch.SpeedSearch
import com.intellij.ui.speedSearch.SpeedSearchSupply
import com.intellij.util.containers.JBIterable
import java.awt.Container
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.beans.PropertyChangeEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.KeyStroke

internal open class VimNavigationDispatcher<T : JComponent>(final override val component: T, private val rootNode: KeyStrokeNode.Parent<VimNavigationDispatcher<T>>) : DumbAwareAction(), ComponentHolder {
	companion object {
		private val DISPOSABLE = ApplicationManager.getApplication().getService(PluginDisposableService::class.java)
		private val ENTER_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
		
		private fun findOriginalEnterAction(component: JComponent): WrappedAction? {
			var originalEnterAction: WrappedAction? = null
			
			for (container in JBIterable.generate<Container>(component) { it.parent }) {
				if (container !is JComponent) {
					continue
				}
				
				container.getActionForKeyStroke(ENTER_KEY)?.let {
					originalEnterAction = WrappedAction.ForActionListener(container, it)
				}
				
				for (action in ActionUtil.getActions(container)) {
					if (action.shortcutSet.shortcuts.any { it is KeyboardShortcut && it.firstKeyStroke == ENTER_KEY && it.secondKeyStroke == null }) {
						originalEnterAction = WrappedAction.ForAnAction(action)
					}
				}
			}
			
			return originalEnterAction
		}
		
		@Suppress("UnstableApiUsage")
		fun JComponent.getParentToolWindowId(): String? {
			return InternalDecoratorImpl.findNearestDecorator(this)?.toolWindowId
		}
	}
	
	private val originalEnterAction = findOriginalEnterAction(component)
	private var currentNode: KeyStrokeNode.Parent<VimNavigationDispatcher<T>> = rootNode
	var isSearching = AtomicBoolean(false)
	
	init {
		registerCustomShortcutSet(KeyStrokeNode.getAllShortcuts(getAllKeyStrokes()), component, DISPOSABLE)
		SpeedSearchSupply.getSupply(component, true)?.addChangeListener(::handleSpeedSearchChange)
	}
	
	protected fun getAllKeyStrokes(): Set<KeyStroke> {
		return KeyStrokeNode.getAllKeyStrokes(rootNode, setOf(ENTER_KEY))
	}
	
	private fun handleSpeedSearchChange(e: PropertyChangeEvent) {
		if (e.propertyName == SpeedSearchSupply.ENTERED_PREFIX_PROPERTY_NAME) {
			val speedSearch = e.source as? SpeedSearchSupply ?: return
			
			ApplicationManager.getApplication().invokeLater {
				if (!speedSearch.isPopupActive) {
					isSearching.set(false)
					currentNode = rootNode
				}
			}
		}
	}
	
	final override fun actionPerformed(e: AnActionEvent) {
		val keyEvent = e.inputEvent as? KeyEvent ?: return
		
		if (keyEvent.id == KeyEvent.KEY_PRESSED && keyEvent.keyCode == KeyEvent.VK_ENTER) {
			handleEnterKeyPress(e, keyEvent)
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
	
	private fun handleEnterKeyPress(actionEvent: AnActionEvent, keyEvent: KeyEvent) {
		if (isSearching.compareAndSet(true, false)) {
			when (val supply = SpeedSearchSupply.getSupply(component)) {
				is SpeedSearchBase<*> -> supply.hidePopup()
				is SpeedSearch        -> supply.reset()
			}
		}
		else {
			currentNode = rootNode
			originalEnterAction?.perform(actionEvent, keyEvent)
		}
	}
	
	final override fun update(e: AnActionEvent) {
		e.presentation.isEnabled = !ignoreEventDueToActiveSearch(e) && !ignoreEventDueToActiveEditing(e)
	}
	
	private fun ignoreEventDueToActiveSearch(e: AnActionEvent): Boolean {
		return isSearching.get() && !e.inputEvent.let { it is KeyEvent && it.id == KeyEvent.KEY_PRESSED && it.keyCode == KeyEvent.VK_ENTER }
	}
	
	private fun ignoreEventDueToActiveEditing(e: AnActionEvent): Boolean {
		// Avoid stealing keys from inline text fields.
		return e.dataContext.getData(CommonDataKeys.EDITOR) != null
	}
	
	final override fun getActionUpdateThread(): ActionUpdateThread {
		return ActionUpdateThread.BGT
	}
	
	private sealed interface WrappedAction {
		fun perform(actionEvent: AnActionEvent, keyEvent: KeyEvent)
		
		class ForActionListener(val component: JComponent, val listener: ActionListener) : WrappedAction {
			override fun perform(actionEvent: AnActionEvent, keyEvent: KeyEvent) {
				listener.actionPerformed(ActionEvent(component, ActionEvent.ACTION_PERFORMED, "Enter", keyEvent.`when`, keyEvent.modifiersEx))
			}
		}
		
		class ForAnAction(val action: AnAction) : WrappedAction {
			override fun perform(actionEvent: AnActionEvent, keyEvent: KeyEvent) {
				action.actionPerformed(actionEvent)
			}
		}
	}
}
