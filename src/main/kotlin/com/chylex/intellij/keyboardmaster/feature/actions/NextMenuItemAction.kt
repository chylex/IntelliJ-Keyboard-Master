package com.chylex.intellij.keyboardmaster.feature.actions

import javax.swing.JList
import javax.swing.JTree

class NextMenuItemAction : SelectMenuItemBaseAction() {
	override fun updateSelection(list: JList<*>) {
		setSelectedIndex(list, list.selectedIndex + 1)
	}
	
	override fun updateSelection(tree: JTree) {
		val row = tree.selectionRows?.maxOrNull()
		if (row != null) {
			setSelectedIndex(tree, row + 1)
		}
	}
}
