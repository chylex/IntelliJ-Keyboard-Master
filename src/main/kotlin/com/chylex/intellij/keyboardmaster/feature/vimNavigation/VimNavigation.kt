package com.chylex.intellij.keyboardmaster.feature.vimNavigation

import com.chylex.intellij.keyboardmaster.PluginDisposableService
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimListNavigation
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimTableNavigation
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimTreeNavigation
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.getUserData
import com.intellij.openapi.ui.popup.util.PopupUtil
import com.intellij.openapi.ui.putUserData
import com.intellij.openapi.util.Key
import com.intellij.util.ui.StartupUiUtil
import java.awt.AWTEvent
import java.awt.event.FocusEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JTable
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.UIManager

object VimNavigation {
	private val KEY_INSTALLED = Key.create<Boolean>("KeyboardMaster-VimNavigation-Installed")
	
	private val isEnabledFlag = AtomicBoolean(false)
	private var originalPopupBindings: Array<*>? = null
	
	val isEnabled
		get() = isEnabledFlag.get()
	
	fun register() {
		val disposable = ApplicationManager.getApplication().getService(PluginDisposableService::class.java)
		
		StartupUiUtil.addAwtListener(AWTEvent.FOCUS_EVENT_MASK, disposable, ::handleEvent)
	}
	
	fun setEnabled(enabled: Boolean) {
		if (!isEnabledFlag.compareAndSet(!enabled, enabled)) {
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
		if (event is FocusEvent && event.id == FocusEvent.FOCUS_GAINED && isEnabled) {
			when (val source = event.source) {
				is JList<*> -> installTo(source, VimListNavigation::install)
				is JTree    -> installTo(source, VimTreeNavigation::install)
				is JTable   -> installTo(source, VimTableNavigation::install)
			}
		}
	}
	
	private inline fun <T : JComponent> installTo(component: T, installer: (T) -> Unit) {
		if (component.getUserData(KEY_INSTALLED) == null) {
			component.putUserData(KEY_INSTALLED, true)
			
			if (PopupUtil.getPopupContainerFor(component) == null) {
				installer(component)
			}
		}
	}
}
