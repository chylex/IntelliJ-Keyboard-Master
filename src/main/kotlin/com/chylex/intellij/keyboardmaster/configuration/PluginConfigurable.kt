package com.chylex.intellij.keyboardmaster.configuration

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class PluginConfigurable : Configurable {
	private lateinit var component: JComponent
	
	private val charFields = Array(10) {
		JBTextField(5)
	}
	
	override fun getDisplayName(): String {
		return "Keyboard Master"
	}
	
	override fun createComponent(): JComponent {
		var builder = FormBuilder.createFormBuilder()
		
		for (index in 1..9) {
			builder = builder.addLabeledComponent("Code completion char $index: ", charFields[index])
		}
		
		builder = builder.addLabeledComponent("Code completion next page char: ", charFields[0])
		builder = builder.addComponentFillVertically(JPanel(), 0)
		component = builder.panel
		
		return component
	}
	
	override fun isModified(): Boolean {
		return true
	}
	
	override fun apply() {
		fun getChar(index: Int): Int {
			return charFields[index].text.firstOrNull()?.code ?: 0
		}
		
		val instance = PluginConfiguration.instance
		instance.charOption1 = getChar(1)
		instance.charOption2 = getChar(2)
		instance.charOption3 = getChar(3)
		instance.charOption4 = getChar(4)
		instance.charOption5 = getChar(5)
		instance.charOption6 = getChar(6)
		instance.charOption7 = getChar(7)
		instance.charOption8 = getChar(8)
		instance.charOption9 = getChar(9)
		instance.charNextPage = getChar(0)
		
		PluginConfiguration.update()
	}
	
	override fun reset() {
		fun setChar(index: Int, char: Int) {
			charFields[index].text = if (char == 0) "" else char.toChar().toString()
		}
		
		val instance = PluginConfiguration.instance
		setChar(1, instance.charOption1)
		setChar(2, instance.charOption2)
		setChar(3, instance.charOption3)
		setChar(4, instance.charOption4)
		setChar(5, instance.charOption5)
		setChar(6, instance.charOption6)
		setChar(7, instance.charOption7)
		setChar(8, instance.charOption8)
		setChar(9, instance.charOption9)
		setChar(0, instance.charNextPage)
	}
}
