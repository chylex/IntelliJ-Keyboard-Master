package com.chylex.intellij.keyboardmaster.feature.actions

import javax.swing.JList

class NextMenuItemAction : SelectMenuItemBaseAction() {
	override fun updateSelection(list: JList<*>) {
		setSelectedIndex(list, list.selectedIndex + 1)
	}
}
