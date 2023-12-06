// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
@file:Suppress("ReplaceGetOrSet", "ReplacePutWithAssignment")

package com.jetbrains.ide.streamdeck.customization

import com.intellij.ide.IdeBundle
import com.intellij.ide.ui.customization.CustomActionsSchema
import com.intellij.ide.ui.customization.CustomizableActionGroupProvider
import com.intellij.ide.ui.customization.CustomizableActionGroupProvider.CustomizableActionGroupRegistrar
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.keymap.impl.ui.ActionsTreeUtil
import com.intellij.openapi.keymap.impl.ui.Group
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.IconLoader.getDisabledIcon
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.NaturalComparator
import com.intellij.openapi.wm.ex.WindowManagerEx
import com.intellij.serviceContainer.NonInjectable
import com.intellij.ui.ExperimentalUI
import com.intellij.util.IconUtil
import com.intellij.util.SmartList
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.containers.UnmodifiableHashMap
import com.intellij.util.ui.EmptyIcon
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jdom.Element
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls
import java.io.FileNotFoundException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode

private val LOG = logger<CustomActionsSchema>()

//private val EP_NAME = ExtensionPointName<CustomizableActionGroupProvider>("com.intellij.customizableActionGroupProvider")


/**
 * @param path absolute path to the icon file, url of the icon file or url of the icon file inside jar.
 * Also, the path can contain '_dark', '@2x', '@2x_dark' suffixes, but the resulting icon will be taken
 * according to current scale and UI theme.
 */
@ApiStatus.Internal
@Throws(Throwable::class)
fun loadCustomIcon(path: String): Icon {
  val independentPath = FileUtil.toSystemIndependentName(path)

  val lastDotIndex = independentPath.lastIndexOf('.')
  val rawUrl: String
  val ext: String
  if (lastDotIndex == -1) {
    rawUrl = independentPath
    ext = "svg"
  }
  else {
    rawUrl = independentPath.substring(0, lastDotIndex)
    ext = independentPath.substring(lastDotIndex + 1)
  }

  val possibleSuffixes = listOf("@2x_dark", "_dark@2x", "_dark", "@2x")
  val adjustedUrl = possibleSuffixes.firstOrNull { rawUrl.endsWith(it) }?.let { rawUrl.removeSuffix(it) } ?: rawUrl
  try {
    return doLoadCustomIcon("$adjustedUrl.$ext")
  }
  catch (t: Throwable) {
    // In Light theme we do not fall back on dark icon, so if the original provided path ends with '_dark'
    // and there is no icon file without '_dark' suffix, we will fail.
    // And in this case, we just need to load the file chosen by the user.
    if (rawUrl == adjustedUrl) {
      throw t
    }
    else {
      return doLoadCustomIcon("$rawUrl.$ext")
    }
  }
}

private fun doLoadCustomIcon(urlString: String): Icon {
  if (!urlString.startsWith("file:") && !urlString.startsWith("jar:")) {
    val file = Path.of(urlString)
    if (Files.notExists(file)) {
      throw FileNotFoundException("Failed to find icon by URL: $urlString")
    }

    val icon = IconLoader.findUserIconByPath(file)
    val w = icon.iconWidth
    val h = icon.iconHeight
    if (w <= 1 || h <= 1) {
      throw FileNotFoundException("Failed to find icon by URL: $urlString")
    }

    if (w > EmptyIcon.ICON_18.iconWidth || h > EmptyIcon.ICON_18.iconHeight) {
      return icon.scale(scale = EmptyIcon.ICON_18.iconWidth / w.coerceAtLeast(h).toFloat())
    }
    return icon
  }

  val url = URL(null, urlString)
  val icon = IconLoader.findIcon(url) ?: throw FileNotFoundException("Failed to find icon by URL: $url")
  val w = icon.iconWidth
  val h = icon.iconHeight
  if (w <= 1 || h <= 1) {
    throw FileNotFoundException("Failed to find icon by URL: $url")
  }

  if (w > EmptyIcon.ICON_18.iconWidth || h > EmptyIcon.ICON_18.iconHeight) {
    val scale = EmptyIcon.ICON_18.iconWidth / w.coerceAtLeast(h).toFloat()
    // ScaledResultIcon will be returned here, so we will be unable to scale it again or get the dark version,
    // but we have nothing to do because the icon is too large
    return IconUtil.scale(icon, scale = scale, ancestor = null)
  }
  return icon
}

internal fun getIconForPath(actionManager: ActionManager, iconPath: String): Icon? {
  val reuseFrom = actionManager.getAction(iconPath)
  if (reuseFrom != null) {
    return getOriginalIconFrom(reuseFrom)
  }
  else {
    try {
      return loadCustomIcon(iconPath)
    }
    catch (e: Throwable) {
      LOG.info(e.message)
      return null
    }
  }
}

internal fun getOriginalIconFrom(reuseFrom: AnAction): Icon? {
  val presentation = reuseFrom.templatePresentation
  return presentation.getClientProperty(CustomActionsSchema.PROP_ORIGINAL_ICON) ?: presentation.icon
}

