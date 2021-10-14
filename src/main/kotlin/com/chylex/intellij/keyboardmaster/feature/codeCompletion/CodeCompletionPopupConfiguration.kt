package com.chylex.intellij.keyboardmaster.feature.codeCompletion

import com.chylex.intellij.keyboardmaster.configuration.PluginConfiguration
import com.intellij.util.containers.IntIntHashMap

object CodeCompletionPopupConfiguration {
	const val SHORTCUT_NONE = -1
	const val SHORTCUT_NEXT_PAGE = 0
	
	private val charToShortcutMap = IntIntHashMap(16, SHORTCUT_NONE)
	private var hintTexts = mutableListOf<String>()
	
	val itemShortcutCount
		get() = hintTexts.size
	
	init {
		PluginConfiguration.load()
	}
	
	fun updateShortcuts(itemShortcutChars: String, nextPageShortcutCode: Int) {
		charToShortcutMap.clear()
		if (nextPageShortcutCode != 0) {
			charToShortcutMap[nextPageShortcutCode] = SHORTCUT_NEXT_PAGE
		}
		for ((index, char) in itemShortcutChars.withIndex()) {
			charToShortcutMap[char.code] = index + 1
		}
		
		hintTexts.clear()
		for (char in itemShortcutChars) {
			hintTexts.add(" [$char]")
		}
	}
	
	fun getShortcut(char: Char): Int {
		return charToShortcutMap[char.code]
	}
	
	fun getHintText(index: Int): String {
		return hintTexts[index]
	}
}
