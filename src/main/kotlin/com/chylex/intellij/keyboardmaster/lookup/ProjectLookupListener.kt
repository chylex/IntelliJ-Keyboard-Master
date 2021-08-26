package com.chylex.intellij.keyboardmaster.lookup

import com.chylex.intellij.keyboardmaster.configuration.PluginConfiguration
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
		private val OFFSET_KEY = Key.create<Int>("chylexKeyboardMasterOffset")
		private val IS_MODIFIED_KEY = Key.create<Boolean>("chylexKeyboardMasterModified")
		
		fun getLookupOffset(lookup: LookupImpl): Int {
			val offset = lookup.getUserData(OFFSET_KEY)
			if (offset == null || offset >= lookup.list.model.size) {
				return 0
			}
			else {
				return offset
			}
		}
		
		fun setLookupOffset(lookup: LookupImpl, newOffset: Int) {
			lookup.putUserData(OFFSET_KEY, newOffset)
		}
	}
	
	override fun activeLookupChanged(oldLookup: Lookup?, newLookup: Lookup?) {
		if (newLookup !is LookupImpl || newLookup.getUserData(IS_MODIFIED_KEY) == true) {
			return
		}
		
		newLookup.putUserData(IS_MODIFIED_KEY, true)
		
		@Suppress("UnstableApiUsage")
		newLookup.addPresentationCustomizer { item, presentation ->
			val itemList = newLookup.list.model
			val itemCount = itemList.size
			val offset = getLookupOffset(newLookup)
			
			for (index in 0 until 9) {
				val itemIndex = offset + index
				if (itemIndex >= itemCount) {
					break
				}
				
				if (item === itemList.getElementAt(itemIndex)) {
					val hint = PluginConfiguration.hintText[index]
					if (hint != "") {
						val customized = LookupElementPresentation()
						customized.copyFrom(presentation)
						customized.appendTailTextItalic(hint, true)
						return@addPresentationCustomizer customized
					}
				}
			}
			
			presentation
		}
	}
}
