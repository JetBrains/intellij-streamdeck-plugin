/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "com.jetbrains.ide.streamdeck.plugin"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}