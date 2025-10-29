package com.chylex.intellij.keyboardmaster.feature.vimNavigation

import com.chylex.intellij.keyboardmaster.feature.vimNavigation.VimNavigationDispatcher.WrappedAction.ForKeyListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.SystemInfo
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
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.KeyStroke

internal open class VimNavigationDispatcher<T : JComponent>(final override val component: T, private val rootNode: KeyStrokeNode.Parent<VimNavigationDispatcher<T>>, disposable: Disposable? = null) : DumbAwareAction(), ComponentHolder {
	companion object {
		@JvmStatic
		protected val ENTER_KEY: KeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
		
		private val CTRL_ENTER_KEY: KeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK)
		private val META_ENTER_KEY: KeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.META_DOWN_MASK)
		
		private fun findOriginalEnterAction(component: JComponent): WrappedAction {
			var originalEnterAction: WrappedAction? = null
			
			for (container in JBIterable.generate<Container>(component) { it.parent }) {
				if (container !is JComponent) {
					continue
				}
				
				container.getActionForKeyStroke(ENTER_KEY)?.takeUnless(::isIgnoredEnterAction)?.let {
					originalEnterAction = WrappedAction.ForActionListener(container, it)
				}
				
				for (action in ActionUtil.getActions(container)) {
					if (action.shortcutSet.shortcuts.any { it is KeyboardShortcut && it.firstKeyStroke == ENTER_KEY && it.secondKeyStroke == null }) {
						originalEnterAction = WrappedAction.ForAnAction(action)
					}
				}
			}
			
			return originalEnterAction ?: ForKeyListener(component)
		}
		
		private fun isIgnoredEnterAction(action: ActionListener): Boolean {
			return action is Action && action.getValue(Action.NAME) == "toggle"
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
		registerCustomShortcutSet(KeyStrokeNode.getAllShortcuts(getAllKeyStrokes()), component, disposable)
		SpeedSearchSupply.getSupply(component, true)?.addChangeListener(::handleSpeedSearchChange)
	}
	
	protected fun getAllKeyStrokes(): Set<KeyStroke> {
		return KeyStrokeNode.getAllKeyStrokes(rootNode, setOf(ENTER_KEY, CTRL_ENTER_KEY, META_ENTER_KEY))
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
		handleEnterKeyPress(keyEvent) { originalEnterAction.perform(actionEvent, it) }
	}
	
	protected inline fun handleEnterKeyPress(keyEvent: KeyEvent, originalAction: (KeyEvent) -> Unit) {
		if (isSearching.compareAndSet(true, false) && !runEnterActionImmediately(keyEvent)) {
			stopSpeedSearch()
		}
		else {
			currentNode = rootNode
			originalAction(keyEvent)
		}
	}
	
	private fun runEnterActionImmediately(keyEvent: KeyEvent): Boolean {
		return if (SystemInfo.isMac) keyEvent.isMetaDown else keyEvent.isControlDown
	}
	
	protected open fun stopSpeedSearch() {
		when (val supply = SpeedSearchSupply.getSupply(component)) {
			is SpeedSearchBase<*> -> supply.hidePopup()
			is SpeedSearch        -> supply.reset()
		}
	}
	
	final override fun update(e: AnActionEvent) {
		if (!isSearching.get() && SpeedSearchSupply.getSupply(component)?.isPopupActive == true) {
			isSearching.set(true)
		}
		
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
		return ActionUpdateThread.EDT
	}
	
	private sealed interface WrappedAction {
		fun perform(actionEvent: AnActionEvent, keyEvent: KeyEvent)
		
		class ForActionListener(private val component: JComponent, private val listener: ActionListener) : WrappedAction {
			override fun perform(actionEvent: AnActionEvent, keyEvent: KeyEvent) {
				listener.actionPerformed(ActionEvent(component, ActionEvent.ACTION_PERFORMED, "Enter", keyEvent.`when`, keyEvent.modifiersEx))
			}
		}
		
		class ForAnAction(val action: AnAction) : WrappedAction {
			override fun perform(actionEvent: AnActionEvent, keyEvent: KeyEvent) {
				action.actionPerformed(actionEvent)
			}
		}
		
		class ForKeyListener(private val component: JComponent) : WrappedAction {
			override fun perform(actionEvent: AnActionEvent, keyEvent: KeyEvent) {
				val unconsumedKeyEvent = KeyEvent(component, keyEvent.id, keyEvent.`when`, keyEvent.modifiersEx, keyEvent.keyCode, keyEvent.keyChar, keyEvent.keyLocation)
				component.dispatchEvent(unconsumedKeyEvent)
			}
		}
	}
}
