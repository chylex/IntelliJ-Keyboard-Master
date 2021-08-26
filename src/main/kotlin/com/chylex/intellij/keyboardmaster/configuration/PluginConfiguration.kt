package com.chylex.intellij.keyboardmaster.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.containers.IntIntHashMap
import com.intellij.util.xmlb.XmlSerializerUtil
import it.unimi.dsi.fastutil.ints.Int2IntMap

@State(
	name = "com.chylex.intellij.keyboardmaster.configuration.PluginConfiguration",
	storages = [Storage("KeyboardMaster.xml")]
)
class PluginConfiguration : PersistentStateComponent<PluginConfiguration> {
	var charOption1: Int = '1'.code
	var charOption2: Int = '2'.code
	var charOption3: Int = '3'.code
	var charOption4: Int = '4'.code
	var charOption5: Int = '5'.code
	var charOption6: Int = '6'.code
	var charOption7: Int = '7'.code
	var charOption8: Int = '8'.code
	var charOption9: Int = '9'.code
	var charNextPage: Int = '0'.code
	
	companion object {
		val instance: PluginConfiguration
			get() = ApplicationManager.getApplication().getService(PluginConfiguration::class.java)
		
		val charToIndexMap: Int2IntMap = IntIntHashMap(10, -1)
		val hintText = Array(10) { "" }
		
		init {
			instance
			update()
		}
		
		fun update() {
			val instance = instance
			
			charToIndexMap.clear()
			charToIndexMap[instance.charOption1] = 1
			charToIndexMap[instance.charOption2] = 2
			charToIndexMap[instance.charOption3] = 3
			charToIndexMap[instance.charOption4] = 4
			charToIndexMap[instance.charOption5] = 5
			charToIndexMap[instance.charOption6] = 6
			charToIndexMap[instance.charOption7] = 7
			charToIndexMap[instance.charOption8] = 8
			charToIndexMap[instance.charOption9] = 9
			charToIndexMap[instance.charNextPage] = 0
			
			for (entry in charToIndexMap.int2IntEntrySet()) {
				val hintIndex = if (entry.intValue == 0) 9 else entry.intValue - 1
				val charText = if (entry.intKey == 0) "" else " [${entry.intKey.toChar()}]"
				hintText[hintIndex] = charText
			}
		}
	}
	
	override fun getState(): PluginConfiguration {
		return this
	}
	
	override fun loadState(state: PluginConfiguration) {
		XmlSerializerUtil.copyBean(state, this)
	}
}
