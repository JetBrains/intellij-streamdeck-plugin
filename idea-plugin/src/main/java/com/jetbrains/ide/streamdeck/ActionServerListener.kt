// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.ide.streamdeck

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic

/**
 * Listens to changes to global StreamDeck action server.  [ActionServerListener.getInstance].
 *
 * Use [ActionServerListener.subscribe] to start listening to changes or
 * [ActionServerListener.fireServerStatusChanged] to notify all listeners about changes.
 */
interface ActionServerListener {

  /**
   * Is called when global action schema is changed.
   *
   * So toolbars can be dynamically updated according to these changes.
   */
  fun statusChanged()

  companion object {
    private val TOPIC = Topic.create("StreamDeck Action Server Status changed", ActionServerListener::class.java)

    /**
     * Subscribe for changes in global action schema.
     */
    @JvmStatic
    fun subscribe(disposable: Disposable, listener: ActionServerListener) {
      ApplicationManager.getApplication().messageBus.connect(disposable).subscribe(TOPIC, listener)
    }

    /**
     * Notify all listeners about global server status/log changes.
     */
    @JvmStatic
    fun fireServerStatusChanged() {
      ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).statusChanged()
    }
  }

}