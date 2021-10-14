package com.chylex.intellij.keyboardmaster

import com.chylex.intellij.keyboardmaster.feature.codeCompletion.CodeCompletionPopupKeyHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class PluginStartup : StartupActivity.DumbAware {
	private var isInitialized = false
	
	override fun runActivity(project: Project) {
		if (!isInitialized) {
			isInitialized = true
			
			val application = ApplicationManager.getApplication()
			if (application.isUnitTestMode) {
				CodeCompletionPopupKeyHandler.registerRawHandler()
			}
			else {
				application.invokeLater(CodeCompletionPopupKeyHandler.Companion::registerRawHandler)
			}
		}
	}
}
