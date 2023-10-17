package com.chylex.intellij.keyboardmaster.feature.action.gotoError

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.daemon.impl.actions.GotoNextErrorAction

class GotoNextErrorInOtherModeAction : GotoNextErrorAction() {
	override fun getHandler(): CodeInsightActionHandler {
		return GotoErrorInOtherModeHandler(forward = true)
	}
}
