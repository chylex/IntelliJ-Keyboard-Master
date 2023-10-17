package com.chylex.intellij.keyboardmaster.feature.action.gotoType

import com.intellij.codeInsight.CodeInsightActionHandler

class GotoPreviousTypeInFileAction : AbstractGotoTypeInFileAction() {
	override fun getHandler(): CodeInsightActionHandler {
		return GotoTypeInFileHandler(forward = false)
	}
}
