package com.chylex.intellij.keyboardmaster.lookup

import com.intellij.codeInsight.lookup.LookupFocusDegree
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.template.impl.editorActions.TypedActionHandlerBase
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.ui.ScrollingUtil

/**
 * When typing digits 1-9 inside a code completion popup menu, selects the n-th item in the list.
 * When typing the digit 0, moves down the list by 9 items, wrapping around if needed.
 */
class LookupTypedActionHandler(originalHandler: TypedActionHandler?) : TypedActionHandlerBase(originalHandler) {
	override fun execute(editor: Editor, charTyped: Char, dataContext: DataContext) {
		if (!executeImpl(editor, charTyped)) {
			myOriginalHandler?.execute(editor, charTyped, dataContext)
		}
	}
	
	private fun executeImpl(editor: Editor, charTyped: Char): Boolean {
		val shortcutItem = ProjectLookupListener.getShortcut(charTyped)
		if (shortcutItem == -1) {
			return false
		}
		
		val lookup = LookupManager.getActiveLookup(editor)
		if (lookup !is LookupImpl) {
			return false
		}
		
		val offset = ProjectLookupListener.getLookupOffset(lookup)
		
		if (shortcutItem == 0) {
			val list = lookup.list
			val itemCount = list.model.size
			
			val shortcutCount = ProjectLookupListener.itemShortcutCount
			val topIndex = (offset + shortcutCount).let { if (it >= itemCount) 0 else it }
			
			ProjectLookupListener.setLookupOffset(lookup, topIndex)
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
