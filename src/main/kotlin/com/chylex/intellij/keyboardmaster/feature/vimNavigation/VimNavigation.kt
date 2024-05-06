package com.chylex.intellij.keyboardmaster.feature.vimNavigation

import com.chylex.intellij.keyboardmaster.PluginDisposableService
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimListNavigation
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimTableNavigation
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimTreeNavigation
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.UiInterceptors
import com.intellij.util.ui.StartupUiUtil
import java.awt.AWTEvent
import java.awt.event.FocusEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JList
import javax.swing.JTable
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.UIManager

object VimNavigation {
	private val isEnabled = AtomicBoolean(false)
	private var originalPopupBindings: Array<*>? = null
	
	fun register() {
		val disposable = ApplicationManager.getApplication().getService(PluginDisposableService::class.java)
		
		StartupUiUtil.addAwtListener(::handleEvent, AWTEvent.FOCUS_EVENT_MASK, disposable)
		UiInterceptors.registerPersistent(disposable, PopupInterceptor)
	}
	
	fun setEnabled(enabled: Boolean) {
		if (!isEnabled.compareAndSet(!enabled, enabled)) {
			return
		}
		
		ApplicationManager.getApplication().invokeLater {
			if (enabled) {
				val originalBindings = (UIManager.get("PopupMenu.selectedWindowInputMapBindings") as Array<*>).also { originalPopupBindings = it }
				val updatedBindings = mutableListOf(*originalBindings)
				
				addBinding(updatedBindings, "selectParent", setOf('h', 'p', 'x'))
				addBinding(updatedBindings, "selectNext", setOf('j'))
				addBinding(updatedBindings, "selectPrevious", setOf('k'))
				addBinding(updatedBindings, "selectChild", setOf('l', 'o'))
				addBinding(updatedBindings, "cancel", setOf('q'))
				
				UIManager.put("PopupMenu.selectedWindowInputMapBindings", updatedBindings.toTypedArray())
				UISettings.getInstance().disableMnemonics = true
			}
			else {
				UIManager.put("PopupMenu.selectedWindowInputMapBindings", originalPopupBindings)
			}
		}
	}
	
	private fun addBinding(bindings: MutableList<Any?>, action: String, chars: Set<Char>) {
		for (char in chars) {
			bindings.add(KeyStroke.getKeyStroke(char))
			bindings.add(action)
		}
	}
	
	private fun handleEvent(event: AWTEvent) {
		if (event is FocusEvent && event.id == FocusEvent.FOCUS_GAINED && isEnabled.get()) {
			when (val source = event.source) {
				is JList<*> -> VimListNavigation.install(source)
				is JTree    -> VimTreeNavigation.install(source)
				is JTable   -> VimTableNavigation.install(source)
			}
		}
	}
}
