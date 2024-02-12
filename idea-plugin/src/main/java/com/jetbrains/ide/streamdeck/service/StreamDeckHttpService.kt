@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")
// https://youtrack.jetbrains.com/issue/KTIJ-20816/Bogus-error-Cannot-inline-bytecode-built-with-JVM-target-11-into-bytecode-that-is-being-built-with-JVM-target-1.8.#focus=Comments-27-7378342.0-0

package com.jetbrains.ide.streamdeck.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.ide.streamdeck.ActionServerListener.Companion.fireServerStatusChanged
import com.jetbrains.ide.streamdeck.settings.ActionServerSettings
import com.jetbrains.ide.streamdeck.util.ActionExecutor
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.QueryStringDecoder
import org.jetbrains.ide.RestService
import org.jetbrains.io.response
import java.text.SimpleDateFormat
import java.util.*

/**
 * @api {get} /action Execute an IDE action.
 */
internal class StreamDeckHttpService : RestService() {
  companion object {
    @JvmField
    val serverLog = StringBuffer()
  }

  private val defaultDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS ")
  override fun getServiceName() = "action"

  override fun isOriginAllowed(request: HttpRequest): OriginCheckResult {
    val originAllowed = super.isOriginAllowed(request)
//    if (originAllowed == OriginCheckResult.FORBID && request.isEduToolsPluginRelated()) {
//      return OriginCheckResult.ALLOW
//    }
    return originAllowed
  }

  fun sendError(out: String, request: HttpRequest, context: ChannelHandlerContext) {
    var bytes = out.toByteArray()
    val response = response("application/json", Unpooled.wrappedBuffer(bytes, 0, bytes.size))
    response.setStatus(HttpResponseStatus.FORBIDDEN)
    sendResponse(request, context, response)
  }

  private fun log(msg: String) {
    val message = defaultDateFormat.format(Calendar.getInstance().time) + msg + "\n"
    print(message)
    serverLog.append(message)
    fireServerStatusChanged()
  }

  override fun execute(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    if(!ActionServerSettings.getInstance().enable) return null
    val byteOut = BufferExposingByteArrayOutputStream()
    log("stream deck req ${request.uri()}")
    request.uri().substring("/api/action/".length)
    val passwordHeader = request.headers().getAsString("Authorization")
    val password = ActionServerSettings.getInstance().password
    if (StringUtil.isNotEmpty(password)) {
      if (password != passwordHeader) {
        log("Bad password provided, abort")
        sendError("Bad password provided", request, context)
        return null
      }
    }

    ActionExecutor.performActionUrl(request.uri(), !ActionServerSettings.getInstance().focusOnly)

    send(byteOut, request, context)
    return null
  }
}

