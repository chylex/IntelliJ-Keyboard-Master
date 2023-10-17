package com.chylex.intellij.keyboardmaster.feature.action.gotoError

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.daemon.impl.actions.GotoPreviousErrorAction

class GotoPreviousErrorInOtherModeAction : GotoPreviousErrorAction() {
	override fun getHandler(): CodeInsightActionHandler {
		return GotoErrorInOtherModeHandler(forward = false)
	}
}
