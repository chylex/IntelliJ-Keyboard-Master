package com.chylex.intellij.keyboardmaster.feature.vimNavigation.components

import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.ActionNode
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.IdeaAction
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.KeyStrokeNode.Parent
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.VimNavigationDispatcher
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimCommonNavigation.findScrollPane
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimCommonNavigation.scrollBy
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimCommonNavigation.scrollByPages
import com.chylex.intellij.keyboardmaster.feature.vimNavigation.components.VimCommonNavigation.withShiftModifier
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.getUserData
import com.intellij.openapi.ui.putUserData
import com.intellij.openapi.util.Key
import com.intellij.ui.ClientProperty
import com.intellij.ui.tree.ui.DefaultTreeUI
import java.awt.event.KeyEvent
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.tree.TreeModel
import javax.swing.tree.TreePath

internal object VimTreeNavigation {
	private val KEY = Key.create<VimNavigationDispatcher<JTree>>("KeyboardMaster-VimTreeNavigation")
	
	private val ROOT_NODE = VimCommonNavigation.commonRootNode<JTree>() + Parent(
		mapOf(
			KeyStroke.getKeyStroke('a') to Parent(
				mapOf(
					KeyStroke.getKeyStroke('o') to ExpandAll,
					KeyStroke.getKeyStroke('x') to CollapseAll,
				)
			),
			KeyStroke.getKeyStroke('g') to Parent(
				mapOf(
					KeyStroke.getKeyStroke('g') to IdeaAction("Tree-selectFirst"),
					KeyStroke.getKeyStroke('j') to IdeaAction("Tree-selectNextSibling"),
					KeyStroke.getKeyStroke('k') to IdeaAction("Tree-selectPreviousSibling"),
				)
			),
			KeyStroke.getKeyStroke('G') to IdeaAction("Tree-selectLast"),
			KeyStroke.getKeyStroke('h') to CollapseSelfOrMoveToParentNode,
			KeyStroke.getKeyStroke('H') to CollapseUntilRootNode,
			KeyStroke.getKeyStroke('j') to IdeaAction("Tree-selectNext"),
			KeyStroke.getKeyStroke('J') to IdeaAction("Tree-selectNextExtendSelection"),
			KeyStroke.getKeyStroke('k') to IdeaAction("Tree-selectPrevious"),
			KeyStroke.getKeyStroke('K') to IdeaAction("Tree-selectPreviousExtendSelection"),
			KeyStroke.getKeyStroke('l') to ExpandSelfOrMoveToFirstChildNode,
			KeyStroke.getKeyStroke('L') to ExpandUntilFirstLeafNode,
			KeyStroke.getKeyStroke('o') to ExpandChildrenToNextLevel,
			KeyStroke.getKeyStroke('O') to IdeaAction("FullyExpandTreeNode"),
			KeyStroke.getKeyStroke('p') to IdeaAction("Tree-selectParentNoCollapse"),
			KeyStroke.getKeyStroke('P') to IdeaAction("Tree-selectFirst"),
			KeyStroke.getKeyStroke('s') to SelectFirstSibling,
			KeyStroke.getKeyStroke('S') to SelectLastSibling,
			KeyStroke.getKeyStroke('x') to CollapseChildrenToPreviousLevel,
			KeyStroke.getKeyStroke('X') to CollapseSelf,
			*withShiftModifier(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK) { ScrollVerticallyAndSelect(pages = -1.0F, extendSelection = it) },
			*withShiftModifier(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK) { ScrollVerticallyAndSelect(pages = +0.5F, extendSelection = it) },
			*withShiftModifier(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK) { ScrollVerticallyAndSelect(pages = +1.0F, extendSelection = it) },
			*withShiftModifier(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK) { ScrollVerticallyAndSelect(pages = -0.5F, extendSelection = it) },
		)
	)
	
	fun install(component: JTree) {
		if (component.getUserData(KEY) == null) {
			component.putUserData(KEY, VimNavigationDispatcher(component, ROOT_NODE))
		}
	}
	
	private data class ScrollVerticallyAndSelect(private val pages: Float, private val extendSelection: Boolean) : ActionNode<VimNavigationDispatcher<JTree>> {
		override fun performAction(holder: VimNavigationDispatcher<JTree>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val tree = holder.component
			val scrollPane = tree.findScrollPane() ?: return
			
			scrollPane.scrollByPages(pages)
			
			if (pages < 0F) {
				val topPath = pathOnTop(tree) ?: return
				val topPathBounds = tree.getPathBounds(topPath) ?: return
				scrollPane.scrollBy(topPathBounds.height - 1)
			}
			
			val pathToSelect = pathOnTop(tree) ?: return
			
			if (extendSelection) {
				val anchor = tree.anchorSelectionPath
				val anchorRow = if (anchor == null) -1 else tree.getRowForPath(anchor)
				if (anchorRow < 0) {
					tree.selectionPath = pathToSelect
				}
				else {
					tree.setSelectionInterval(tree.getRowForPath(pathToSelect), anchorRow)
					tree.setAnchorSelectionPath(anchor)
					tree.leadSelectionPath = pathToSelect
				}
			}
			else {
				tree.selectionPath = pathToSelect
			}
			
			tree.scrollRectToVisible(tree.getPathBounds(pathToSelect))
		}
		
		private fun pathOnTop(tree: JTree): TreePath? {
			return tree.visibleRect.let { tree.getClosestPathForLocation(it.x, it.y) }
		}
	}
	
	private data object CollapseSelf : ActionNode<VimNavigationDispatcher<JTree>> {
		override fun performAction(holder: VimNavigationDispatcher<JTree>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val tree = holder.component
			val path = tree.selectionPath ?: return
			
			collapseAndScroll(tree, path)
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
			
			var row = 0
			
			while (row < tree.rowCount) {
				tree.collapseRow(row)
				row++
			}
		}
	}
	
	private data object CollapseChildrenToPreviousLevel : ActionNode<VimNavigationDispatcher<JTree>> {
		override fun performAction(holder: VimNavigationDispatcher<JTree>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val tree = holder.component
			val model = tree.model
			val path = tree.selectionPath?.takeIf(tree::isExpanded) ?: return
			
			var currentLevel = mutableListOf(path)
			
			while (true) {
				val nextLevel = mutableListOf<TreePath>()
				
				for (parentPath in currentLevel) {
					forEachChild(model, parentPath) {
						val childPath = parentPath.pathByAddingChild(it)
						if (tree.isExpanded(childPath)) {
							nextLevel.add(childPath)
						}
					}
				}
				
				if (nextLevel.isNotEmpty()) {
					currentLevel = nextLevel
				}
				else {
					break
				}
			}
			
			for (parentPath in currentLevel) {
				tree.collapsePath(parentPath)
			}
		}
	}
	
	private data object ExpandAll : ActionNode<VimNavigationDispatcher<JTree>> {
		override fun performAction(holder: VimNavigationDispatcher<JTree>, actionEvent: AnActionEvent, keyEvent: KeyEvent) {
			val tree = holder.component
			
			var row = 0
			
			while (row < tree.rowCount) {
				tree.expandRow(row)
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
					
					for (path in pathsToExpand) {
						forEachChild(model, path) { tree.collapsePath(path.pathByAddingChild(it)) }
					}
					
					break
				}
				
				val nextPathsToExpand = mutableListOf<TreePath>()
				
				for (parentPath in pathsToExpand) {
					forEachChild(model, parentPath) {
						if (!model.isLeaf(it)) {
							nextPathsToExpand.add(parentPath.pathByAddingChild(it))
						}
					}
				}
				
				pathsToExpand = nextPathsToExpand
			} while (pathsToExpand.isNotEmpty())
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
	
	private inline fun forEachChild(model: TreeModel, path: TreePath, action: (Any) -> Unit) {
		val lastPathComponent = path.lastPathComponent
		
		for (i in 0 until model.getChildCount(lastPathComponent)) {
			action(model.getChild(lastPathComponent, i))
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
		tree.scrollPathToVisible(path)
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
