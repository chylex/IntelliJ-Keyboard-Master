package com.chylex.intellij.keyboardmaster.feature.vimNavigation.components

import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.ActionNode
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.IdeaAction
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.Parent
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.VimNavigationDispatcher
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.VimNavigationDispatcher.Companion.getParentToolWindowId
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ex.ToolWindowManagerEx
import com.intellij.ui.ComponentUtil
import com.intellij.ui.speedSearch.SpeedSearchActivator
import com.intellij.ui.speedSearch.SpeedSearchSupply
import java.awt.Point
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JScrollPane
import javax.swing.JViewport
import javax.swing.KeyStroke

internal object VimCommonNavigation {
	fun <T : JComponent> commonRootNode() = Parent<VimNavigationDispatcher<T>>(
		mapOf(
			KeyStroke.getKeyStroke('A') to IdeaAction("MaximizeToolWindow"),
			KeyStroke.getKeyStroke('f') to StartSearch(),
			KeyStroke.getKeyStroke('I') to ToggleExcludedFilesInProjectView(),
			KeyStroke.getKeyStroke('m') to IdeaAction("ShowPopupMenu"),
			KeyStroke.getKeyStroke('r') to IdeaAction("SynchronizeCurrentFile"),
			KeyStroke.getKeyStroke('R') to IdeaAction("Synchronize"),
			KeyStroke.getKeyStroke('q') to CloseParentPopupOrToolWindow(),
			KeyStroke.getKeyStroke('/') to StartSearch(),
		)
	)
	
	inline fun <T> withShiftModifier(keyCode: Int, modifiers: Int, constructor: (Boolean) -> KeyStrokeNode<T>): Array<Pair<KeyStroke, KeyStrokeNode<T>>> {
		return arrayOf(
			KeyStroke.getKeyStroke(keyCode, modifiers) to constructor(false),
			KeyStroke.getKeyStroke(keyCode, modifiers or KeyEvent.SHIFT_DOWN_MASK) to constructor(true),
		)
	}
	
	private class StartSearch<T : JComponent> : ActionNode<VimNavigationDispatcher<T>> {
		@Suppress("UnstableApiUsage")
		override fun performAction(holder: VimNavigationDispatcher<T>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val speedSearch = SpeedSearchSupply.getSupply(holder.component, true) as? SpeedSearchActivator ?: return
			if (speedSearch.isAvailable) {
				holder.isSearching.set(true)
				speedSearch.activate()
			}
		}
	}
	
	private class CloseParentPopupOrToolWindow<T : JComponent> : ActionNode<VimNavigationDispatcher<T>> {
		override fun performAction(holder: VimNavigationDispatcher<T>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val popup = holder.popup
			if (popup != null) {
				popup.cancel()
				return
			}
			
			val project = actionEvent.project ?: return
			val toolWindowId = holder.component.getParentToolWindowId() ?: return
			ToolWindowManagerEx.getInstanceEx(project).hideToolWindow(toolWindowId, true)
		}
	}
	
	private class ToggleExcludedFilesInProjectView<T : JComponent> : ActionNode<VimNavigationDispatcher<T>> {
		private val showExcludedFilesAction = IdeaAction<VimNavigationDispatcher<T>>("ProjectView.ShowExcludedFiles")
		
		override fun performAction(holder: VimNavigationDispatcher<T>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			if (holder.component.getParentToolWindowId() == ToolWindowId.PROJECT_VIEW) {
				showExcludedFilesAction.performAction(holder, actionEvent, keyEvent)
			}
		}
	}
	
	fun JComponent.findScrollPane(): JScrollPane? {
		return ComponentUtil.getParentOfType(JScrollPane::class.java, this)
	}
	
	fun JScrollPane.scrollByPages(pages: Float) {
		scrollBy(this) { (it.height.toFloat() * pages).toInt() }
	}
	
	fun JScrollPane.scrollBy(amount: Int) {
		scrollBy(this) { amount }
	}
	
	private inline fun scrollBy(scrollPane: JScrollPane, amount: (JViewport) -> Int) {
		val viewport = scrollPane.viewport
		
		val position = viewport.viewPosition
		val scrollTo = (position.y + amount(viewport)).coerceIn(0, viewport.viewSize.height - viewport.height)
		
		viewport.viewPosition = Point(position.x, scrollTo)
	}
}
