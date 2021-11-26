package com.chylex.intellij.keyboardmaster.feature.actions

import javax.swing.JList

class PrevMenuItemAction : SelectMenuItemBaseAction() {
	override fun updateSelection(list: JList<*>) {
		val index = list.selectedIndex
		if (index == -1) {
			setSelectedIndex(list, list.model.size - 1)
		}
		else {
			setSelectedIndex(list, index - 1)
		}
	}
}
