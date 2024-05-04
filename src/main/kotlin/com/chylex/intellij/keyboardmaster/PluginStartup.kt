package com.chylex.intellij.keyboardmaster

import com.chylex.intellij.keyboardmaster.configuration.PluginConfiguration
import com.chylex.intellij.keyboardmaster.feature.codeCompletion.CodeCompletionPopupKeyHandler
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.VimNavigation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class PluginStartup : ProjectActivity {
	private var isInitialized = false
	
	override suspend fun execute(project: Project) {
		if (!isInitialized) {
			isInitialized = true
			
			PluginConfiguration.load()
			
			val application = ApplicationManager.getApplication()
			if (application.isUnitTestMode) {
				initialize()
			}
			else {
				application.invokeLater(::initialize)
			}
		}
	}
	
	private fun initialize() {
		CodeCompletionPopupKeyHandler.registerRawHandler()
		VimNavigation.register()
	}
}
