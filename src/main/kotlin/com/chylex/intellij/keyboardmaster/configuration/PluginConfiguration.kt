package com.chylex.intellij.keyboardmaster.configuration

import com.chylex.intellij.keyboardmaster.feature.codeCompletion.CodeCompletionPopupConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
	name = "com.chylex.intellij.keyboardmaster.configuration.PluginConfiguration",
	storages = [Storage("KeyboardMaster.xml")]
)
class PluginConfiguration : PersistentStateComponent<PluginConfiguration> {
	var codeCompletionItemShortcuts = "123456789"
	var codeCompletionNextPageShortcut: Int = '0'.code
	var codeCompletionPrevPageShortcut: Int = 0
	
	companion object {
		private val instance: PluginConfiguration
			get() = ApplicationManager.getApplication().getService(PluginConfiguration::class.java)
		
		fun load() {
			instance
		}
		
		fun read(callback: (PluginConfiguration) -> Unit) {
			instance.apply(callback)
		}
		
		fun modify(callback: (PluginConfiguration) -> Unit) {
			instance.apply(callback).apply(this::update)
		}
		
		private fun update(instance: PluginConfiguration) = with(instance) {
			CodeCompletionPopupConfiguration.updateShortcuts(codeCompletionItemShortcuts, codeCompletionNextPageShortcut, codeCompletionPrevPageShortcut)
		}
	}
	
	override fun getState(): PluginConfiguration {
		return this
	}
	
	override fun loadState(state: PluginConfiguration) {
		XmlSerializerUtil.copyBean(state, this)
		update(this)
	}
}
