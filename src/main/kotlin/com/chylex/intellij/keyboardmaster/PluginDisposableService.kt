package com.chylex.intellij.keyboardmaster

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service

@Service
internal class PluginDisposableService : Disposable {
	override fun dispose() {}
}
