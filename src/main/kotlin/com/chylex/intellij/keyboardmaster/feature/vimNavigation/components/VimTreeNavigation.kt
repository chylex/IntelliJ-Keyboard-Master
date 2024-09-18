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
					KeyStroke.getKeyStroke('o') to ExpandChildrenToNextLevel,
				)
			),
			KeyStroke.getKeyStroke('G') to IdeaAction("Tree-selectLast"),
			KeyStroke.getKeyStroke('h') to CollapseSelfOrMoveToParentNode,
			KeyStroke.getKeyStroke('H') to CollapseUntilRootNode,
			KeyStroke.getKeyStroke('j') to IdeaAction("Tree-selectNext"),
			KeyStroke.getKeyStroke('j', KeyEvent.ALT_DOWN_MASK) to IdeaAction("Tree-selectNextSibling"),
			KeyStroke.getKeyStroke('J') to IdeaAction("Tree-selectNextExtendSelection"),
			KeyStroke.getKeyStroke('k') to IdeaAction("Tree-selectPrevious"),
			KeyStroke.getKeyStroke('k', KeyEvent.ALT_DOWN_MASK) to IdeaAction("Tree-selectPreviousSibling"),
			KeyStroke.getKeyStroke('K') to IdeaAction("Tree-selectPreviousExtendSelection"),
			KeyStroke.getKeyStroke('l') to ExpandSelfOrMoveToFirstChildNode,
			KeyStroke.getKeyStroke('L') to ExpandUntilFirstLeafNode,
			KeyStroke.getKeyStroke('o') to ExpandOrCollapseTreeNode,
			KeyStroke.getKeyStroke('O') to IdeaAction("FullyExpandTreeNode"),
			KeyStroke.getKeyStroke('p') to IdeaAction("Tree-selectParentNoCollapse"),
			KeyStroke.getKeyStroke('P') to IdeaAction("Tree-selectFirst"),
			KeyStroke.getKeyStroke('x') to CollapseSelfOrParentNode,
			KeyStroke.getKeyStroke('X') to CollapseAll,
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
				runWithoutAutoExpand(tree) { tree.expandPath(path) }
			}
		}
	}
	
	private data object ExpandSelfOrMoveToFirstChildNode : ActionNode<VimNavigationDispatcher<JTree>> {
		override fun performAction(holder: VimNavigationDispatcher<JTree>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val tree = holder.component
			val path = tree.selectionPath?.takeUnless { isLeaf(tree, it) } ?: return
			
			if (tree.isExpanded(path)) {
				selectRow(tree, getFirstChild(tree, path))
			}
			else {
				runWithoutAutoExpand(tree) { tree.expandPath(path) }
			}
		}
	}
	
	private data object ExpandUntilFirstLeafNode : ActionNode<VimNavigationDispatcher<JTree>> {
		override fun performAction(holder: VimNavigationDispatcher<JTree>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val tree = holder.component
			val path = tree.selectionPath ?: return
			
			var firstChildPath = path
			
			while (!isLeaf(tree, firstChildPath)) {
				tree.expandPath(firstChildPath)
				firstChildPath = getFirstChild(tree, firstChildPath)
			}
			
			selectRow(tree, firstChildPath)
		}
	}
	
	private data object CollapseSelfOrMoveToParentNode : ActionNode<VimNavigationDispatcher<JTree>> {
		override fun performAction(holder: VimNavigationDispatcher<JTree>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val tree = holder.component
			val path = tree.selectionPath ?: return
			
			if (tree.isExpanded(path)) {
				collapseAndScroll(tree, path)
			}
			else {
				withParentPath(tree, path) { selectRow(tree, it) }
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
				withParentPath(tree, path) { collapseAndScroll(tree, it) }
			}
		}
	}
	
	private data object CollapseUntilRootNode : ActionNode<VimNavigationDispatcher<JTree>> {
		override fun performAction(holder: VimNavigationDispatcher<JTree>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val tree = holder.component
			val path = tree.selectionPath ?: return
			
			var parentPath = path
			
			while (true) {
				parentPath = parentPath.parentPath.takeUnless { isInvisibleRoot(tree, it) } ?: break
			}
			
			collapseAndScroll(tree, parentPath)
		}
	}
	
	private data object CollapseAll : ActionNode<VimNavigationDispatcher<JTree>> {
		override fun performAction(holder: VimNavigationDispatcher<JTree>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val tree = holder.component
			
			CollapseUntilRootNode.performAction(holder, actionEvent, keyEvent)
			
			var row = 0
			
			while (row < tree.rowCount) {
				tree.collapseRow(row)
				row++
			}
		}
	}
	
	private data object ExpandChildrenToNextLevel : ActionNode<VimNavigationDispatcher<JTree>> {
		override fun performAction(holder: VimNavigationDispatcher<JTree>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val tree = holder.component
			val model = tree.model
			val path = tree.selectionPath?.takeUnless { isLeaf(tree, it) } ?: return
			
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
	
	private inline fun runWithoutAutoExpand(tree: JTree, action: () -> Unit) {
		val previousAutoExpandValue = ClientProperty.get(tree, DefaultTreeUI.AUTO_EXPAND_ALLOWED)
		ClientProperty.put(tree, DefaultTreeUI.AUTO_EXPAND_ALLOWED, false)
		try {
			action()
		} finally {
			ClientProperty.put(tree, DefaultTreeUI.AUTO_EXPAND_ALLOWED, previousAutoExpandValue)
		}
	}
	
	private fun selectRow(tree: JTree, row: Int) {
		tree.setSelectionRow(row)
		tree.scrollRowToVisible(row)
	}
	
	private fun selectRow(tree: JTree, path: TreePath) {
		selectRow(tree, tree.getRowForPath(path))
	}
	
	private fun collapseAndScroll(tree: JTree, path: TreePath) {
		tree.collapsePath(path)
		tree.scrollRowToVisible(tree.getRowForPath(path))
	}
	
	private inline fun withParentPath(tree: JTree, path: TreePath, action: (TreePath) -> Unit) {
		val parentPath = path.parentPath
		if (!isInvisibleRoot(tree, parentPath)) {
			action(parentPath)
		}
	}
	
	private fun isInvisibleRoot(tree: JTree, parentPath: TreePath): Boolean {
		return parentPath.parentPath == null && !tree.isRootVisible
	}
	
	private fun getFirstChild(tree: JTree, path: TreePath): TreePath {
		return path.pathByAddingChild(tree.model.getChild(path.lastPathComponent, 0))
	}
	
	private fun isLeaf(tree: JTree, firstChildPath: TreePath): Boolean {
		return tree.model.isLeaf(firstChildPath.lastPathComponent)
	}
}
