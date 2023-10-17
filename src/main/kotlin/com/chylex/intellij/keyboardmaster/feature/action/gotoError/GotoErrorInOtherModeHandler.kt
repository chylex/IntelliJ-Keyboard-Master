package com.chylex.intellij.keyboardmaster.feature.action.gotoError

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.codeInsight.daemon.impl.GotoNextErrorHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class GotoErrorInOtherModeHandler(forward: Boolean) : GotoNextErrorHandler(forward) {
	override fun invoke(project: Project, editor: Editor, file: PsiFile) {
		val settings = DaemonCodeAnalyzerSettings.getInstance()
		val oldMode = settings.isNextErrorActionGoesToErrorsFirst
		
		settings.isNextErrorActionGoesToErrorsFirst = !oldMode
		try {
			super.invoke(project, editor, file)
		} finally {
			settings.isNextErrorActionGoesToErrorsFirst = oldMode
		}
	}
}
