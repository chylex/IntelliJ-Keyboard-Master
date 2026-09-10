package com.chylex.intellij.keyboardmaster.feature.vimNavigation.components

import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.ActionNode
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.IdeaAction
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.Parent
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.VimNavigationDispatcher
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimCommonNavigation.findScrollPane
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimCommonNavigation.scrollBy
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimCommonNavigation.scrollByPages
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimCommonNavigation.withShiftModifier
import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.getUserData
import com.intellij.openapi.ui.putUserData
import com.intellij.openapi.util.Key
import com.intellij.vcs.log.ui.table.VcsLogGraphTable
import java.awt.Rectangle
import java.awt.event.KeyEvent
import javax.swing.JTable
import javax.swing.KeyStroke

internal object VimTableNavigation {
	private val KEY = Key.create<VimNavigationDispatcher<JTable>>("KeyboardMaster-VimTableNavigation")
	
	private val BASIC_ROOT_NODE = VimCommonNavigation.commonRootNode<JTable>() + Parent(
		mapOf(
			KeyStroke.getKeyStroke('g') to IdeaAction("Table-selectFirstRow"),
			KeyStroke.getKeyStroke('G') to IdeaAction("Table-selectLastRow"),
			KeyStroke.getKeyStroke('h') to IdeaAction("Table-selectPreviousColumn"),
			KeyStroke.getKeyStroke('H') to IdeaAction("Table-selectPreviousColumnExtendSelection"),
			KeyStroke.getKeyStroke('j') to IdeaAction("Table-selectNextRow"),
			KeyStroke.getKeyStroke('J') to IdeaAction("Table-selectNextRowExtendSelection"),
			KeyStroke.getKeyStroke('k') to IdeaAction("Table-selectPreviousRow"),
			KeyStroke.getKeyStroke('K') to IdeaAction("Table-selectPreviousRowExtendSelection"),
			KeyStroke.getKeyStroke('l') to IdeaAction("Table-selectNextColumn"),
			KeyStroke.getKeyStroke('L') to IdeaAction("Table-selectNextColumnExtendSelection"),
			*withShiftModifier(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK) { ScrollVerticallyAndSelect(pages = -1.0F, extendSelection = it) },
			*withShiftModifier(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK) { ScrollVerticallyAndSelect(pages = +0.5F, extendSelection = it) },
			*withShiftModifier(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK) { ScrollVerticallyAndSelect(pages = +1.0F, extendSelection = it) },
			*withShiftModifier(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK) { ScrollVerticallyAndSelect(pages = -0.5F, extendSelection = it) },
		)
	)
	
	private val GIT_LOG_TABLE_ROOT_NODE = BASIC_ROOT_NODE + Parent(
		mapOf(
			KeyStroke.getKeyStroke('d') to IdeaAction("Git.Drop.Commits"),
			KeyStroke.getKeyStroke('n') to IdeaAction("Git.Reword.Commit"),
			KeyStroke.getKeyStroke('p') to IdeaAction("Vcs.CherryPick"),
			KeyStroke.getKeyStroke('r') to IdeaAction("Git.Interactive.Rebase"),
			KeyStroke.getKeyStroke('s') to IdeaAction("Git.Squash.Commits"),
			KeyStroke.getKeyStroke('u') to IdeaAction("Git.Uncommit"),
			KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0) to IdeaAction("Git.CheckoutRevision"),
		)
	)
	private val GIT_REBASE_TABLE_ROOT_NODE = BASIC_ROOT_NODE + Parent(
		mapOf(
			KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyEvent.ALT_DOWN_MASK) to DispatchKeyEvent(KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED, KeyEvent.ALT_DOWN_MASK),
			KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.ALT_DOWN_MASK) to DispatchKeyEvent(KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED, KeyEvent.ALT_DOWN_MASK),
			KeyStroke.getKeyStroke('d') to DispatchKeyEvent(KeyEvent.VK_D, 'd', KeyEvent.ALT_DOWN_MASK),
			KeyStroke.getKeyStroke('e') to DispatchKeyEvent(KeyEvent.VK_E, 'e', KeyEvent.ALT_DOWN_MASK),
			KeyStroke.getKeyStroke('f') to DispatchKeyEvent(KeyEvent.VK_F, 'f', KeyEvent.ALT_DOWN_MASK),
			KeyStroke.getKeyStroke('p') to DispatchKeyEvent(KeyEvent.VK_P, 'p', KeyEvent.ALT_DOWN_MASK),
			KeyStroke.getKeyStroke('r') to DispatchKeyEvent(KeyEvent.VK_R, 'r', KeyEvent.ALT_DOWN_MASK),
			KeyStroke.getKeyStroke('s') to DispatchKeyEvent(KeyEvent.VK_S, 's', KeyEvent.ALT_DOWN_MASK),
		)
	)
	
	fun install(component: JTable) {
		if (component.getUserData(KEY) == null) {
			component.putUserData(KEY, VimNavigationDispatcher(component, pickRootNode(component)))
		}
	}
	
	private fun pickRootNode(component: JTable) = when (component) {
		is VcsLogGraphTable -> GIT_LOG_TABLE_ROOT_NODE
		else                -> when {
			component::class.java.name.contains("GitInteractiveRebase") -> GIT_REBASE_TABLE_ROOT_NODE
			else                                                        -> BASIC_ROOT_NODE
		}
	}
	
	private data class ScrollVerticallyAndSelect(private val pages: Float, private val extendSelection: Boolean) : ActionNode<VimNavigationDispatcher<JTable>> {
		override fun performAction(holder: VimNavigationDispatcher<JTable>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val table = holder.component
			val scrollPane = table.findScrollPane() ?: return
			
			scrollPane.scrollByPages(pages)
			
			if (pages < 0F) {
				scrollPane.scrollBy(table.rowHeight - 1)
			}
			
			val visibleRect = table.visibleRect
			val rowIndexToSelect = table.rowAtPoint(visibleRect.location)
			if (rowIndexToSelect == -1) {
				return
			}
			
			val rowRect = table.getCellRect(rowIndexToSelect, 0, true)
			val adjustedRect = Rectangle(visibleRect.x, rowRect.y, visibleRect.width, visibleRect.height)
			
			table.changeSelection(rowIndexToSelect, table.selectedColumn, false, extendSelection)
			table.scrollRectToVisible(adjustedRect)
		}
	}
	
	private data class DispatchKeyEvent(private val keyCode: Int, private val keyChar: Char, private val modifiers: Int) : ActionNode<VimNavigationDispatcher<JTable>> {
		override fun performAction(holder: VimNavigationDispatcher<JTable>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			with(IdeEventQueue.getInstance().keyEventDispatcher) {
				dispatchKeyEvent(KeyEvent(holder.component, KeyEvent.KEY_PRESSED, keyEvent.`when`, modifiers, keyCode, keyChar))
				dispatchKeyEvent(KeyEvent(holder.component, KeyEvent.KEY_RELEASED, keyEvent.`when`, modifiers, keyCode, keyChar))
			}
		}
	}
}
