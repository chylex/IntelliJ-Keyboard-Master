package com.chylex.intellij.keyboardmaster.feature.vimNavigation.components

import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.ActionNode
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.IdeaAction
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.Parent
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.VimNavigationDispatcher
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.VimNavigationDispatcher.Companion.getParentToolWindowId
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ex.ToolWindowManagerEx
import com.intellij.ui.speedSearch.SpeedSearchActivator
import com.intellij.ui.speedSearch.SpeedSearchSupply
import java.awt.event.KeyEvent
import javax.swing.JComponent
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
}
