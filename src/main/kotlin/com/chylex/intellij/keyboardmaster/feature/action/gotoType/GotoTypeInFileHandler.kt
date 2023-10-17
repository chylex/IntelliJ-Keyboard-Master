package com.chylex.intellij.keyboardmaster.feature.action.gotoType

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.lang.LanguageStructureViewBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import it.unimi.dsi.fastutil.ints.IntArrayList

class GotoTypeInFileHandler(private val forward: Boolean) : CodeInsightActionHandler {
	override fun invoke(project: Project, editor: Editor, file: PsiFile) {
		LookupManager.getInstance(project).hideActiveLookup()
		
		val caretOffset = editor.caretModel.offset
		val caretLine = editor.caretModel.logicalPosition.line
		
		val searchedOffsetRange = if (forward)
			caretOffset + 1..file.textLength
		else
			0 until caretOffset
		
		val navigationOffsets = getNavigationOffsets(file, searchedOffsetRange)
		if (!forward) {
			navigationOffsets.reverse()
		}
		
		val direction = if (forward) 1 else -1
		for (offset in navigationOffsets) {
			val line = editor.offsetToLogicalPosition(offset).line
			if (line.compareTo(caretLine) * direction > 0) {
				editor.caretModel.removeSecondaryCarets()
				editor.caretModel.moveToOffset(offset)
				editor.selectionModel.removeSelection()
				editor.scrollingModel.scrollToCaret(if (forward) ScrollType.CENTER_DOWN else ScrollType.CENTER_UP)
				IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation()
				break
			}
		}
	}
	
	override fun getElementToMakeWritable(currentFile: PsiFile): PsiElement? {
		return null
	}
	
	private companion object {
		fun getNavigationOffsets(file: PsiFile, searchedOffsetRange: IntRange): IntArray {
			val structureViewBuilder = LanguageStructureViewBuilder.INSTANCE.getStructureViewBuilder(file)
			if (structureViewBuilder !is TreeBasedStructureViewBuilder) {
				return intArrayOf()
			}
			
			val elements = mutableSetOf<PsiElement>()
			val model = structureViewBuilder.createStructureViewModel(null)
			
			try {
				addStructureViewElements(elements, model.root, file)
			} finally {
				Disposer.dispose(model)
			}
			
			return offsetsFromElements(elements, searchedOffsetRange)
		}
		
		private fun addStructureViewElements(result: MutableSet<PsiElement>, parent: StructureViewTreeElement, file: PsiFile) {
			for (child in parent.children) {
				val value = (child as StructureViewTreeElement).value
				if (value is PsiClass && file == value.containingFile) {
					result.add(value)
				}
				
				addStructureViewElements(result, child, file)
			}
		}
		
		private fun offsetsFromElements(elements: Collection<PsiElement>, searchedOffsetRange: IntRange): IntArray {
			val offsets = IntArrayList(elements.size)
			
			for (element in elements) {
				val offset = element.textOffset
				if (offset in searchedOffsetRange) {
					offsets.add(offset)
				}
			}
			
			offsets.sort(null)
			return offsets.toIntArray()
		}
	}
}
