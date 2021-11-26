package com.chylex.intellij.keyboardmaster.feature.actions

import javax.swing.JList
import javax.swing.JTree

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
	
	override fun updateSelection(tree: JTree) {
		val row = tree.selectionRows?.minOrNull()
		if (row != null) {
			setSelectedIndex(tree, row - 1)
		}
	}
}
