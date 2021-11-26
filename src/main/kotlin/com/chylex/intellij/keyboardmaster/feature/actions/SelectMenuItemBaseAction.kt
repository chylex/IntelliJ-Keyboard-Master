package com.chylex.intellij.keyboardmaster.feature.actions

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.ide.actions.BigPopupUI
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.ComponentUtil
import java.awt.KeyboardFocusManager
import javax.swing.JList

abstract class SelectMenuItemBaseAction internal constructor(): DumbAwareAction() {
	init {
		isEnabledInModalContext = true
	}
	
	final override fun actionPerformed(e: AnActionEvent) {
		val editor = e.getData(CommonDataKeys.EDITOR)
		if (editor != null) {
			val lookup = LookupManager.getActiveLookup(editor)
			if (lookup is LookupImpl) {
				updateSelection(lookup.list)
				return
			}
		}
		
		var focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
		while (focused != null) {
			if (focused is JList<*>) {
				updateSelection(focused)
				break
			}
			else if (focused is BigPopupUI) {
				val list = ComponentUtil.findComponentsOfType(focused, JList::class.java).singleOrNull()
				if (list != null) {
					updateSelection(list)
					break
				}
			}
			
			focused = focused.parent
		}
	}
	
	protected abstract fun updateSelection(list: JList<*>)
	
	protected fun setSelectedIndex(list: JList<*>, newIndex: Int) {
		if (newIndex in 0 until list.model.size) {
			list.selectedIndex = newIndex
			list.ensureIndexIsVisible(newIndex)
		}
	}
}
