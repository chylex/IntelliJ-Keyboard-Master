package com.chylex.intellij.keyboardmaster.feature.action.gotoError

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.daemon.impl.actions.GotoPreviousErrorAction
import com.intellij.openapi.actionSystem.DataContext

class GotoPreviousErrorInOtherModeAction : GotoPreviousErrorAction() {
	override fun getHandler(dataContext: DataContext): CodeInsightActionHandler {
		return GotoErrorInOtherModeHandler(forward = false)
	}
}
