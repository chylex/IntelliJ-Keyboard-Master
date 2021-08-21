package com.chylex.intellij.keyboardmaster.lookup

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.util.Key

/**
 * Adds hints to code completion items with the digit that selects it.
 */
class ProjectLookupListener : LookupManagerListener {
	companion object {
		private val IS_MODIFIED_KEY = Key.create<Boolean>("chylexKeyboardMasterModified")
		private val HINT_TEXT = Array(10) { " [${(it + 1) % 10}]" }
	}
	
	override fun activeLookupChanged(oldLookup: Lookup?, newLookup: Lookup?) {
		if (newLookup !is LookupImpl || newLookup.getUserData(IS_MODIFIED_KEY) == true) {
			return
		}
		
		newLookup.putUserData(IS_MODIFIED_KEY, true)
		
		@Suppress("UnstableApiUsage")
		newLookup.addPresentationCustomizer { item, presentation ->
			val items = newLookup.list.model
			
			for (index in 0 until items.size.coerceAtMost(10)) {
				if (item === items.getElementAt(index)) {
					val customized = LookupElementPresentation()
					customized.copyFrom(presentation)
					customized.appendTailTextItalic(HINT_TEXT[index], true)
					return@addPresentationCustomizer customized
				}
			}
			
			presentation
		}
	}
}
