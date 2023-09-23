package com.jetbrains.ide.streamdeck.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil


@State(name = "ActionServerSettings", storages = [Storage("StreamDeckActionServerSettings.xml")])
internal data class ActionServerSettings(
  var defaultPort: Int = 21420, // Default service listen port, deprecated
  var password: String = "", // Optional password
  var enable: Boolean = true,// Enable service or not
  var focusOnly: Boolean = true,// Perform actions only when IDE window is focused
) : PersistentStateComponent<ActionServerSettings> {

  companion object {
    @JvmStatic
    fun getInstance(): ActionServerSettings = ApplicationManager.getApplication().getService(
      ActionServerSettings::class.java)
  }

  override fun getState(): ActionServerSettings = this

  override fun loadState(state: ActionServerSettings) {
    XmlSerializerUtil.copyBean(state, this)
  }
}
