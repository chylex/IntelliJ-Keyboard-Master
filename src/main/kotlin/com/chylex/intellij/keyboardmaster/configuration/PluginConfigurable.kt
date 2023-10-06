package com.chylex.intellij.keyboardmaster.configuration

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class PluginConfigurable : Configurable {
	private lateinit var component: JComponent
	
	private val codeCompletionItemShortcuts = JBTextField(20)
	private val codeCompletionNextPageShortcut = JBTextField(2)
	private val codeCompletionPrevPageShortcut = JBTextField(2)
	
	override fun getDisplayName(): String {
		return "Keyboard Master"
	}
	
	override fun createComponent(): JComponent {
		component = panel {
			group("Code Completion") {
				row("Item shortcuts:") { cell(codeCompletionItemShortcuts) }
				row("Next page shortcut:") { cell(codeCompletionNextPageShortcut) }
				row("Prev page shortcut:") { cell(codeCompletionPrevPageShortcut) }
			}
		}
		
		return component
	}
	
	override fun isModified(): Boolean {
		return true
	}
	
	override fun apply() {
		PluginConfiguration.modify {
			it.codeCompletionItemShortcuts = codeCompletionItemShortcuts.text
			it.codeCompletionNextPageShortcut = codeCompletionNextPageShortcut.text.firstOrNull()?.code ?: 0
			it.codeCompletionPrevPageShortcut = codeCompletionPrevPageShortcut.text.firstOrNull()?.code ?: 0
		}
	}
	
	override fun reset() {
		PluginConfiguration.read {
			codeCompletionItemShortcuts.text = it.codeCompletionItemShortcuts
			codeCompletionNextPageShortcut.text = it.codeCompletionNextPageShortcut.let { code -> if (code == 0) "" else code.toChar().toString() }
			codeCompletionPrevPageShortcut.text = it.codeCompletionPrevPageShortcut.let { code -> if (code == 0) "" else code.toChar().toString() }
		}
	}
}
