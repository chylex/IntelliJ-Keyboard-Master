package com.chylex.intellij.keyboardmaster.feature.action.gotoError

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.daemon.impl.actions.GotoNextErrorAction
import com.intellij.openapi.actionSystem.DataContext

class GotoNextErrorInOtherModeAction : GotoNextErrorAction() {
	override fun getHandler(dataContext: DataContext): CodeInsightActionHandler {
		return GotoErrorInOtherModeHandler(forward = true)
	}
}
