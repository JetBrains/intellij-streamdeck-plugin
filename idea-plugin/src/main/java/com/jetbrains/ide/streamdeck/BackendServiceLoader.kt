package com.jetbrains.ide.streamdeck

import com.intellij.ide.ApplicationInitializedListener
import com.jetbrains.ide.streamdeck.settings.ActionServerSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class BackendServiceLoader : ApplicationInitializedListener {
    override suspend fun execute(asyncScope: CoroutineScope) {
        withContext(Dispatchers.Default) {
            try {
                if(ActionServerSettings.getInstance().enableRemote) {
                    RemoteActionServer.getInstance().start()
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }
}
