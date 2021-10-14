package com.chylex.intellij.keyboardmaster.feature.codeCompletion

import com.intellij.codeInsight.lookup.LookupFocusDegree
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.template.impl.editorActions.TypedActionHandlerBase
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.ui.ScrollingUtil
import javax.swing.ListModel

/**
 * Handles configured key bindings inside a code completion popup menu.
 */
class CodeCompletionPopupKeyHandler(originalHandler: TypedActionHandler?) : TypedActionHandlerBase(originalHandler) {
	override fun execute(editor: Editor, charTyped: Char, dataContext: DataContext) {
		if (!executeImpl(editor, charTyped)) {
			myOriginalHandler?.execute(editor, charTyped, dataContext)
		}
	}
	
	private fun executeImpl(editor: Editor, charTyped: Char): Boolean {
		val shortcutItem = CodeCompletionPopupConfiguration.getShortcut(charTyped)
		if (shortcutItem == CodeCompletionPopupConfiguration.SHORTCUT_NONE) {
			return false
		}
		
		val lookup = LookupManager.getActiveLookup(editor)
		if (lookup !is LookupImpl) {
			return false
		}
		
		val offset = CodeCompletionPopupListener.getPageOffset(lookup)
		
		if (shortcutItem == CodeCompletionPopupConfiguration.SHORTCUT_NEXT_PAGE) {
			setPageOffset(lookup) {
				val newTopIndex = offset + CodeCompletionPopupConfiguration.itemShortcutCount
				if (newTopIndex >= it.size) 0 else newTopIndex
			}
		}
		else if (shortcutItem == CodeCompletionPopupConfiguration.SHORTCUT_PREV_PAGE) {
			setPageOffset(lookup) {
				val newTopIndex = offset - CodeCompletionPopupConfiguration.itemShortcutCount
				if (newTopIndex < 0) 0 else newTopIndex
			}
		}
		else {
			selectItem(lookup, offset + shortcutItem)
		}
		
		return true
	}
	
	private inline fun setPageOffset(lookup: LookupImpl, getNewTopIndex: (ListModel<*>) -> Int) {
		val list = lookup.list
		val newTopIndex = getNewTopIndex(list.model)
		
		CodeCompletionPopupListener.setPageOffset(lookup, newTopIndex)
		lookup.selectedIndex = newTopIndex
		ScrollingUtil.ensureRangeIsVisible(list, newTopIndex, newTopIndex + CodeCompletionPopupConfiguration.itemShortcutCount - 1)
		lookup.markSelectionTouched()
		lookup.refreshUi(false, true)
	}
	
	private fun selectItem(lookup: LookupImpl, index: Int) {
		if (!lookup.isFocused) {
			lookup.lookupFocusDegree = LookupFocusDegree.FOCUSED
			lookup.refreshUi(false, true)
		}
		
		lookup.selectedIndex = index
	}
}
