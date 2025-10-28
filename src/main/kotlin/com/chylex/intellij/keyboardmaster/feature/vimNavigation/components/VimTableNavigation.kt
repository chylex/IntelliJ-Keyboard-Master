package com.chylex.intellij.keyboardmaster.feature.vimNavigation.components

import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.ActionNode
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.IdeaAction
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.Parent
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.VimNavigationDispatcher
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimCommonNavigation.findScrollPane
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimCommonNavigation.scrollBy
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimCommonNavigation.scrollByPages
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimCommonNavigation.withShiftModifier
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.getUserData
import com.intellij.openapi.ui.putUserData
import com.intellij.openapi.util.Key
import java.awt.Rectangle
import java.awt.event.KeyEvent
import javax.swing.JTable
import javax.swing.KeyStroke

internal object VimTableNavigation {
	private val KEY = Key.create<VimNavigationDispatcher<JTable>>("KeyboardMaster-VimTableNavigation")
	
	private val ROOT_NODE = VimCommonNavigation.commonRootNode<JTable>() + Parent(
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
	
	fun install(component: JTable) {
		if (component.getUserData(KEY) == null) {
			component.putUserData(KEY, VimNavigationDispatcher(component, ROOT_NODE))
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
}
