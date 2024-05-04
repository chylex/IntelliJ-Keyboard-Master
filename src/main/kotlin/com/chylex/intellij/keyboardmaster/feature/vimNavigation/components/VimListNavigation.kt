package com.chylex.intellij.keyboardmaster.feature.vimNavigation.components

import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.IdeaAction
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.Parent
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.VimNavigationDispatcher
import com.intellij.openapi.ui.getUserData
import com.intellij.openapi.ui.putUserData
import com.intellij.openapi.util.Key
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
		if (component.getUserData(KEY) == null) {
			component.putUserData(KEY, VimNavigationDispatcher(component, ROOT_NODE))
		}
	}
}
