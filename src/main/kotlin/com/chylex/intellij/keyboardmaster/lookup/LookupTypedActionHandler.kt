package com.chylex.intellij.keyboardmaster.lookup

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
		if (charTyped !in '0'..'9') {
			return false
		}
		
		val lookup = LookupManager.getActiveLookup(editor)
		if (lookup !is LookupImpl) {
			return false
		}
		
		val offset = ProjectLookupListener.getLookupOffset(lookup)
		
		if (charTyped == '0') {
			val list = lookup.list
			val itemCount = list.model.size
			val topIndex = (offset + 9).let { if (it >= itemCount) 0 else it }
			
			ProjectLookupListener.setLookupOffset(lookup, topIndex)
			lookup.selectedIndex = topIndex
			ScrollingUtil.ensureRangeIsVisible(list, topIndex, topIndex + 8)
			lookup.markSelectionTouched()
			lookup.refreshUi(false, true)
		}
		else {
			lookup.selectedIndex = offset + (charTyped - '1')
		}
		
		return true
	}
}
