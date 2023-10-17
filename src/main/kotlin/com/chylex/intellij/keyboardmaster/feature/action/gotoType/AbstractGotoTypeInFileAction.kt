package com.chylex.intellij.keyboardmaster.feature.action.gotoType

import com.intellij.codeInsight.actions.BaseCodeInsightAction
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.lang.LanguageStructureViewBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

abstract class AbstractGotoTypeInFileAction : BaseCodeInsightAction(), DumbAware {
	init {
		isEnabledInModalContext = true
	}
	
	final override fun isValidForLookup(): Boolean {
		return true
	}
	
	final override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
		return checkValidForFile(file)
	}
	
	private companion object {
		fun checkValidForFile(file: PsiFile): Boolean {
			return try {
				LanguageStructureViewBuilder.INSTANCE.getStructureViewBuilder(file) is TreeBasedStructureViewBuilder
			} catch (e: IndexNotReadyException) {
				false
			}
		}
	}
}
