package com.chylex.intellij.keyboardmaster.feature.vimNavigation

import com.intellij.ui.popup.WizardPopup
import javax.swing.JComponent

internal interface ComponentHolder {
	val component: JComponent
	
	val popup: WizardPopup?
		get() = null
}
