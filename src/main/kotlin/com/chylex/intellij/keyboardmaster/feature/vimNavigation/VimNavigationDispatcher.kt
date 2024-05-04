package com.chylex.intellij.keyboardmaster.feature.vimNavigation

import com.chylex.intellij.keyboardmaster.PluginDisposableService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.pom.Navigatable
import com.intellij.toolWindow.InternalDecoratorImpl
import com.intellij.ui.SpeedSearchBase
import com.intellij.ui.speedSearch.SpeedSearch
import com.intellij.ui.speedSearch.SpeedSearchSupply
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.KeyStroke

internal class VimNavigationDispatcher<T : JComponent>(override val component: T, private val rootNode: KeyStrokeNode.Parent<VimNavigationDispatcher<T>>) : DumbAwareAction(), ComponentHolder {
	companion object {
		private val DISPOSABLE = ApplicationManager.getApplication().getService(PluginDisposableService::class.java)
		private val EXTRA_SHORTCUTS = setOf(
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
		)
		
		@Suppress("UnstableApiUsage")
		fun JComponent.getParentToolWindowId(): String? {
			return InternalDecoratorImpl.findNearestDecorator(this)?.toolWindowId
		}
	}
	
	private var currentNode: KeyStrokeNode.Parent<VimNavigationDispatcher<T>> = rootNode
	var isSearching = AtomicBoolean(false)
	
	init {
		registerCustomShortcutSet(KeyStrokeNode.getAllShortcuts(rootNode, EXTRA_SHORTCUTS), component, DISPOSABLE)
		
		SpeedSearchSupply.getSupply(component, true)?.addChangeListener {
			if (it.propertyName == SpeedSearchSupply.ENTERED_PREFIX_PROPERTY_NAME && it.oldValue != null && it.newValue == null) {
				isSearching.set(false)
			}
		}
	}
	
	override fun actionPerformed(e: AnActionEvent) {
		val keyEvent = e.inputEvent as? KeyEvent ?: return
		
		if (keyEvent.id == KeyEvent.KEY_PRESSED && handleSpecialKeyPress(keyEvent, e.dataContext)) {
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
	
	private fun handleSpecialKeyPress(keyEvent: KeyEvent, dataContext: DataContext): Boolean {
		if (keyEvent.keyCode == KeyEvent.VK_ESCAPE) {
			return true
		}
		
		if (keyEvent.keyCode == KeyEvent.VK_ENTER) {
			handleEnterKeyPress(dataContext)
			return true
		}
		
		return false
	}
	
	private fun handleEnterKeyPress(dataContext: DataContext) {
		if (isSearching.compareAndSet(true, false)) {
			when (val supply = SpeedSearchSupply.getSupply(component)) {
				is SpeedSearchBase<*> -> supply.hidePopup()
				is SpeedSearch        -> supply.reset()
			}
		}
		else {
			val navigatables = dataContext.getData(CommonDataKeys.NAVIGATABLE_ARRAY)?.filter(Navigatable::canNavigate).orEmpty()
			for ((index, navigatable) in navigatables.withIndex()) {
				navigatable.navigate(index == navigatables.lastIndex)
			}
		}
	}
	
	override fun update(e: AnActionEvent) {
		e.presentation.isEnabled = !isSearching.get() || e.inputEvent.let { it is KeyEvent && it.id == KeyEvent.KEY_PRESSED && it.keyCode == KeyEvent.VK_ENTER }
	}
	
	override fun getActionUpdateThread(): ActionUpdateThread {
		return ActionUpdateThread.BGT
	}
}
