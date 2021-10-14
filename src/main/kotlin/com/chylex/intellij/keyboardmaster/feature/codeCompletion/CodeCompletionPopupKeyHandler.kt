package com.chylex.intellij.keyboardmaster.feature.codeCompletion

import com.intellij.codeInsight.lookup.LookupFocusDegree
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.template.impl.editorActions.TypedActionHandlerBase
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.ui.ScrollingUtil

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
		
		val offset = CodeCompletionPopupListener.getLookupOffset(lookup)
		
		if (shortcutItem == CodeCompletionPopupConfiguration.SHORTCUT_NEXT_PAGE) {
			val list = lookup.list
			val itemCount = list.model.size
			
			val shortcutCount = CodeCompletionPopupConfiguration.itemShortcutCount
			val topIndex = (offset + shortcutCount).let { if (it >= itemCount) 0 else it }
			
			CodeCompletionPopupListener.setLookupOffset(lookup, topIndex)
			lookup.selectedIndex = topIndex
			ScrollingUtil.ensureRangeIsVisible(list, topIndex, topIndex + shortcutCount - 1)
			lookup.markSelectionTouched()
			lookup.refreshUi(false, true)
		}
		else {
			if (!lookup.isFocused) {
				lookup.lookupFocusDegree = LookupFocusDegree.FOCUSED
				lookup.refreshUi(false, true)
			}
			
			lookup.selectedIndex = offset + shortcutItem - 1
		}
		
		return true
	}
}
