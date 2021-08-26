package com.chylex.intellij.keyboardmaster.configuration

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import javax.swing.JComponent

class PluginConfigurable : Configurable {
	private lateinit var component: JComponent
	
	private val codeCompletionItemShortcuts = JBTextField(20)
	private val codeCompletionNextPageShortcut = JBTextField(2)
	
	override fun getDisplayName(): String {
		return "Keyboard Master"
	}
	
	override fun createComponent(): JComponent {
		component = panel {
			titledRow("Code Completion") {
				row("Item shortcuts:") { component(codeCompletionItemShortcuts) }
				row("Next page shortcut:") { component(codeCompletionNextPageShortcut) }
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
		}
	}
	
	override fun reset() {
		PluginConfiguration.read {
			codeCompletionItemShortcuts.text = it.codeCompletionItemShortcuts
			codeCompletionNextPageShortcut.text = it.codeCompletionNextPageShortcut.let { code -> if (code == 0) "" else code.toChar().toString() }
		}
	}
}
