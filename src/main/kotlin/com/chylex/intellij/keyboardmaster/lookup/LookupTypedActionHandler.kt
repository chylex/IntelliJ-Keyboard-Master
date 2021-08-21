package com.chylex.intellij.keyboardmaster.lookup

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.template.impl.editorActions.TypedActionHandlerBase
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedActionHandler

/**
 * When typing digits inside a code completion popup menu, selects the n-th item (or 10th item if the digit is 0) in the list.
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
		
		lookup.selectedIndex = if (charTyped == '0') 9 else charTyped - '1'
		return true
	}
}
