package com.chylex.intellij.keyboardmaster.feature.vimNavigation.components

import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.IdeaAction
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.Parent
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.VimNavigationDispatcher
import com.intellij.openapi.ui.getUserData
import com.intellij.openapi.ui.putUserData
import com.intellij.openapi.util.Key
import javax.swing.JTable
import javax.swing.KeyStroke

internal object VimTableNavigation {
	private val KEY = Key.create<VimNavigationDispatcher<JTable>>("KeyboardMaster-VimTableNavigation")
	
	private val ROOT_NODE = VimCommonNavigation.commonRootNode<JTable>() + Parent(
		mapOf(
			KeyStroke.getKeyStroke('g') to IdeaAction("Table-selectFirstRow"),
			KeyStroke.getKeyStroke('G') to IdeaAction("Table-selectLastRow"),
			KeyStroke.getKeyStroke('h') to IdeaAction("Table-selectPreviousColumn"),
			KeyStroke.getKeyStroke('H') to IdeaAction("Table-selectPreviousColumnExtendSelection"),
			KeyStroke.getKeyStroke('j') to IdeaAction("Table-selectNextRow"),
			KeyStroke.getKeyStroke('J') to IdeaAction("Table-selectNextRowExtendSelection"),
			KeyStroke.getKeyStroke('k') to IdeaAction("Table-selectPreviousRow"),
			KeyStroke.getKeyStroke('K') to IdeaAction("Table-selectPreviousRowExtendSelection"),
			KeyStroke.getKeyStroke('l') to IdeaAction("Table-selectNextColumn"),
			KeyStroke.getKeyStroke('L') to IdeaAction("Table-selectNextColumnExtendSelection"),
		)
	)
	
	fun install(component: JTable) {
		if (component.getUserData(KEY) == null) {
			component.putUserData(KEY, VimNavigationDispatcher(component, ROOT_NODE))
		}
	}
}
