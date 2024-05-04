package com.chylex.intellij.keyboardmaster.feature.vimNavigation.components

import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.IdeaAction
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.Parent
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.VimNavigationDispatcher
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.getUserData
import com.intellij.openapi.ui.putUserData
import com.intellij.openapi.util.Key
import com.intellij.ui.popup.WizardPopup
import com.intellij.ui.speedSearch.SpeedSearch
import com.intellij.ui.speedSearch.SpeedSearchSupply
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.JList
import javax.swing.KeyStroke

internal object VimListNavigation {
	private val KEY = Key.create<VimNavigationDispatcher<JList<*>>>("KeyboardMaster-VimListNavigation")
	
	private val ROOT_NODE = VimCommonNavigation.commonRootNode<JList<*>>() + Parent(
		mapOf(
			KeyStroke.getKeyStroke('g') to Parent(
				mapOf(
					KeyStroke.getKeyStroke('g') to IdeaAction("List-selectFirstRow"),
				)
			),
			KeyStroke.getKeyStroke('G') to IdeaAction("List-selectLastRow"),
			KeyStroke.getKeyStroke('h') to IdeaAction("List-selectPreviousColumn"),
			KeyStroke.getKeyStroke('j') to IdeaAction("List-selectNextRow"),
			KeyStroke.getKeyStroke('k') to IdeaAction("List-selectPreviousRow"),
			KeyStroke.getKeyStroke('l') to IdeaAction("List-selectNextColumn"),
		)
	)
	
	fun install(component: JList<*>) {
		if (component.getUserData(KEY) == null && component.javaClass.enclosingClass.let { it == null || !WizardPopup::class.java.isAssignableFrom(it) }) {
			component.putUserData(KEY, VimNavigationDispatcher(component, ROOT_NODE))
		}
	}
	
	fun install(component: JList<*>, popup: WizardPopup) {
		if (component.getUserData(KEY) == null) {
			component.putUserData(KEY, VimPopupListNavigationDispatcher(component, popup))
		}
	}
	
	@Suppress("serial")
	private class VimPopupListNavigationDispatcher(component: JList<*>, override val popup: WizardPopup) : VimNavigationDispatcher<JList<*>>(component, ROOT_NODE) {
		init {
			val speedSearch = SpeedSearchSupply.getSupply(component, true) as? SpeedSearch
			if (speedSearch != null) {
				installSpeedSearch(speedSearch, popup)
			}
		}
		
		private fun installSpeedSearch(speedSearch: SpeedSearch, popup: WizardPopup) {
			val pauseAction = PauseSpeedSearchAction(this, speedSearch)
			
			for (keyStroke in getAllKeyStrokes()) {
				if (keyStroke.keyEventType != KeyEvent.KEY_TYPED) {
					continue
				}
				
				val keyCode = KeyEvent.getExtendedKeyCodeForChar(keyStroke.keyChar.code)
				if (keyCode != KeyEvent.VK_UNDEFINED) {
					popup.registerAction("KeyboardMaster-VimListNavigation-PauseSpeedSearch", KeyStroke.getKeyStroke(keyCode, 0), pauseAction)
					popup.registerAction("KeyboardMaster-VimListNavigation-PauseSpeedSearch", KeyStroke.getKeyStroke(keyCode, KeyEvent.SHIFT_DOWN_MASK), pauseAction)
				}
			}
			
			// WizardPopup only checks key codes against its input map, but key codes may be undefined for some characters.
			popup.registerAction("KeyboardMaster-VimListNavigation-PauseSpeedSearch", KeyStroke.getKeyStroke(KeyEvent.CHAR_UNDEFINED, 0), pauseAction)
			popup.registerAction("KeyboardMaster-VimListNavigation-PauseSpeedSearch", KeyStroke.getKeyStroke(KeyEvent.CHAR_UNDEFINED, KeyEvent.SHIFT_DOWN_MASK), pauseAction)
		}
		
		private class PauseSpeedSearchAction(private val dispatcher: VimNavigationDispatcher<JList<*>>, private val speedSearch: SpeedSearch) : AbstractAction() {
			override fun actionPerformed(e: ActionEvent) {
				if (!dispatcher.isSearching.get()) {
					speedSearch.setEnabled(false)
					ApplicationManager.getApplication().invokeLater { speedSearch.setEnabled(true) }
				}
			}
		}
	}
}
