package com.chylex.intellij.keyboardmaster.lookup

import com.chylex.intellij.keyboardmaster.configuration.PluginConfiguration
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.util.Key
import com.intellij.util.containers.IntIntHashMap

/**
 * Adds hints to code completion items with the digit that selects it.
 */
class ProjectLookupListener : LookupManagerListener {
	companion object {
		private val OFFSET_KEY = Key.create<Int>("chylexKeyboardMasterOffset")
		private val IS_MODIFIED_KEY = Key.create<Boolean>("chylexKeyboardMasterModified")
		
		private var hintTexts = mutableListOf<String>()
		private val charToShortcutMap = IntIntHashMap(16, -1)
		
		val itemShortcutCount
			get() = hintTexts.size
		
		init {
			PluginConfiguration.load()
		}
		
		fun updateShortcuts(configuration: PluginConfiguration) {
			hintTexts.clear()
			for (char in configuration.codeCompletionItemShortcuts) {
				hintTexts.add(" [$char]")
			}
			
			charToShortcutMap.clear()
			configuration.codeCompletionNextPageShortcut.takeUnless { it == 0 }?.let { charToShortcutMap[it] = 0 }
			for ((index, char) in configuration.codeCompletionItemShortcuts.withIndex()) {
				charToShortcutMap[char.code] = index + 1
			}
		}
		
		fun getShortcut(char: Char): Int {
			return charToShortcutMap[char.code]
		}
		
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
		if (newLookup !is LookupImpl || newLookup.getUserData(IS_MODIFIED_KEY) == true || itemShortcutCount == 0) {
			return
		}
		
		newLookup.putUserData(IS_MODIFIED_KEY, true)
		
		@Suppress("UnstableApiUsage")
		newLookup.addPresentationCustomizer { item, presentation ->
			val itemList = newLookup.list.model
			val itemCount = itemList.size
			val offset = getLookupOffset(newLookup)
			
			for (index in hintTexts.indices) {
				val itemIndex = offset + index
				if (itemIndex >= itemCount) {
					break
				}
				
				if (item === itemList.getElementAt(itemIndex)) {
					val customized = LookupElementPresentation()
					customized.copyFrom(presentation)
					customized.appendTailTextItalic(hintTexts[index], true)
					return@addPresentationCustomizer customized
				}
			}
			
			presentation
		}
	}
}
