package com.chylex.intellij.keyboardmaster.feature.vimNavigation

import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimListNavigation
import com.intellij.ui.UiInterceptors.PersistentUiInterceptor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.popup.AbstractPopup
import com.intellij.ui.popup.list.ListPopupImpl

internal object PopupInterceptor : PersistentUiInterceptor<AbstractPopup>(AbstractPopup::class.java) {
	override fun shouldIntercept(component: AbstractPopup): Boolean {
		if (component is ListPopupImpl) {
			VimListNavigation.install(component.list, component)
		}
		
		return false
	}
	
	override fun doIntercept(component: AbstractPopup, owner: RelativePoint?) {}
}
