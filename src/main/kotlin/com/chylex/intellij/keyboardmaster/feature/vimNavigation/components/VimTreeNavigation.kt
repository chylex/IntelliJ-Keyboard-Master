package com.chylex.intellij.keyboardmaster.feature.vimNavigation.components

import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.ActionNode
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.IdeaAction
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.Parent
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.VimNavigationDispatcher
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.getUserData
import com.intellij.openapi.ui.putUserData
import com.intellij.openapi.util.Key
import com.intellij.ui.ClientProperty
import com.intellij.ui.tree.ui.DefaultTreeUI
import java.awt.event.KeyEvent
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.tree.TreePath

internal object VimTreeNavigation {
	private val KEY = Key.create<VimNavigationDispatcher<JTree>>("KeyboardMaster-VimTreeNavigation")
	
	private val ROOT_NODE = VimCommonNavigation.commonRootNode<JTree>() + Parent(
		mapOf(
			KeyStroke.getKeyStroke('g') to Parent(
				mapOf(
					KeyStroke.getKeyStroke('g') to IdeaAction("Tree-selectFirst"),
					KeyStroke.getKeyStroke('j') to SelectLastSibling,
					KeyStroke.getKeyStroke('k') to SelectFirstSibling,
					KeyStroke.getKeyStroke('o') to ExpandTreeNodeChildrenToNextLevel,
				)
			),
			KeyStroke.getKeyStroke('G') to IdeaAction("Tree-selectLast"),
			KeyStroke.getKeyStroke('j') to IdeaAction("Tree-selectNext"),
			KeyStroke.getKeyStroke('j', KeyEvent.ALT_DOWN_MASK) to IdeaAction("Tree-selectNextSibling"),
			KeyStroke.getKeyStroke('J') to IdeaAction("Tree-selectNextExtendSelection"),
			KeyStroke.getKeyStroke('k') to IdeaAction("Tree-selectPrevious"),
			KeyStroke.getKeyStroke('k', KeyEvent.ALT_DOWN_MASK) to IdeaAction("Tree-selectPreviousSibling"),
			KeyStroke.getKeyStroke('K') to IdeaAction("Tree-selectPreviousExtendSelection"),
			KeyStroke.getKeyStroke('o') to ExpandOrCollapseTreeNode,
			KeyStroke.getKeyStroke('O') to IdeaAction("FullyExpandTreeNode"),
			KeyStroke.getKeyStroke('p') to IdeaAction("Tree-selectParentNoCollapse"),
			KeyStroke.getKeyStroke('P') to IdeaAction("Tree-selectFirst"),
			KeyStroke.getKeyStroke('x') to CollapseSelfOrParentNode,
			KeyStroke.getKeyStroke('X') to IdeaAction("CollapseTreeNode"),
		)
	)
	
	fun install(component: JTree) {
		if (component.getUserData(KEY) == null) {
			component.putUserData(KEY, VimNavigationDispatcher(component, ROOT_NODE))
		}
	}
	
	private data object ExpandOrCollapseTreeNode : ActionNode<VimNavigationDispatcher<JTree>> {
		override fun performAction(holder: VimNavigationDispatcher<JTree>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val tree = holder.component
			val path = tree.selectionPath ?: return
			
			if (tree.isExpanded(path)) {
				tree.collapsePath(path)
			}
			else {
				tree.expandPath(path)
			}
		}
	}
	
	private data object CollapseSelfOrParentNode : ActionNode<VimNavigationDispatcher<JTree>> {
		override fun performAction(holder: VimNavigationDispatcher<JTree>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val tree = holder.component
			val path = tree.selectionPath ?: return
			
			if (tree.isExpanded(path)) {
				collapseAndScroll(tree, path)
			}
			else {
				val parentPath = path.parentPath
				if (parentPath.parentPath != null || tree.isRootVisible) {
					collapseAndScroll(tree, parentPath)
				}
			}
		}
		
		private fun collapseAndScroll(tree: JTree, path: TreePath) {
			tree.collapsePath(path)
			tree.scrollRowToVisible(tree.getRowForPath(path))
		}
	}
	
	private data object ExpandTreeNodeChildrenToNextLevel : ActionNode<VimNavigationDispatcher<JTree>> {
		override fun performAction(holder: VimNavigationDispatcher<JTree>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val tree = holder.component
			val model = tree.model
			val path = tree.selectionPath?.takeUnless { model.isLeaf(it.lastPathComponent) } ?: return
			
			var pathsToExpand = mutableListOf(path)
			
			do {
				if (pathsToExpand.any(tree::isCollapsed)) {
					runWithoutAutoExpand(tree) { pathsToExpand.forEach(tree::expandPath) }
					break
				}
				
				val nextPathsToExpand = mutableListOf<TreePath>()
				
				for (parentPath in pathsToExpand) {
					val lastPathComponent = parentPath.lastPathComponent
					
					for (i in 0 until model.getChildCount(lastPathComponent)) {
						val child = model.getChild(lastPathComponent, i)
						if (!model.isLeaf(child)) {
							nextPathsToExpand.add(parentPath.pathByAddingChild(child))
						}
					}
				}
				
				pathsToExpand = nextPathsToExpand
			} while (pathsToExpand.isNotEmpty())
		}
		
		private inline fun runWithoutAutoExpand(tree: JTree, action: () -> Unit) {
			val previousAutoExpandValue = ClientProperty.get(tree, DefaultTreeUI.AUTO_EXPAND_ALLOWED)
			ClientProperty.put(tree, DefaultTreeUI.AUTO_EXPAND_ALLOWED, false)
			try {
				action()
			} finally {
				ClientProperty.put(tree, DefaultTreeUI.AUTO_EXPAND_ALLOWED, previousAutoExpandValue)
			}
		}
	}
	
	private data object SelectFirstSibling : ActionNode<VimNavigationDispatcher<JTree>> {
		override fun performAction(holder: VimNavigationDispatcher<JTree>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val tree = holder.component
			val path = tree.selectionPath ?: return
			
			val parentPath = path.parentPath ?: return
			val parentRow = tree.getRowForPath(parentPath)
			
			selectRow(tree, parentRow + 1)
		}
	}
	
	private data object SelectLastSibling : ActionNode<VimNavigationDispatcher<JTree>> {
		override fun performAction(holder: VimNavigationDispatcher<JTree>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val tree = holder.component
			val path = tree.selectionPath ?: return
			
			val siblingPathCount = path.pathCount
			var testRow = tree.getRowForPath(path)
			var targetRow = testRow
			
			while (true) {
				testRow++
				
				val testPath = tree.getPathForRow(testRow) ?: break
				val testPathCount = testPath.pathCount
				if (testPathCount < siblingPathCount) {
					break
				}
				else if (testPathCount == siblingPathCount) {
					targetRow = testRow
				}
			}
			
			selectRow(tree, targetRow)
		}
	}
	
	private fun selectRow(tree: JTree, row: Int) {
		tree.setSelectionRow(row)
		tree.scrollRowToVisible(row)
	}
}
