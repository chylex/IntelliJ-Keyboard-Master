package com.chylex.intellij.keyboardmaster.feature.vimNavigation

import com.chylex.intellij.keyboardmaster.PluginDisposableService
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimListNavigation
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimTableNavigation
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimTreeNavigation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.UiInterceptors
import com.intellij.util.ui.StartupUiUtil
import java.awt.AWTEvent
import java.awt.event.FocusEvent
import javax.swing.JList
import javax.swing.JTable
import javax.swing.JTree

object VimNavigation {
	@Volatile
	var isEnabled = false
	
	fun register() {
		val disposable = ApplicationManager.getApplication().getService(PluginDisposableService::class.java)
		
		StartupUiUtil.addAwtListener(::handleEvent, AWTEvent.FOCUS_EVENT_MASK, disposable)
		UiInterceptors.registerPersistent(disposable, PopupInterceptor)
	}
	
	private fun handleEvent(event: AWTEvent) {
		if (event is FocusEvent && event.id == FocusEvent.FOCUS_GAINED && isEnabled) {
			when (val source = event.source) {
				is JList<*> -> VimListNavigation.install(source)
				is JTree    -> VimTreeNavigation.install(source)
				is JTable   -> VimTableNavigation.install(source)
			}
		}
	}
}
