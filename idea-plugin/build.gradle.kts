/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java")
  id("org.jetbrains.intellij") version "1.17.2"
  id("org.jetbrains.kotlin.jvm") version "1.9.0"
}

group = "com.jetbrains.ide.streamdeck"
version = "2024.2.0"

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
//  version.set("223.8214.52")
  // 241-EAP-SNAPSHOT 241-EAP-CANDIDATE-SNAPSHOT
//  version.set("241.14494.240")
  version.set("242-EAP-SNAPSHOT")

  type.set("IU") // Target IDE Platform

  plugins.set(listOf("org.intellij.plugins.markdown", "com.intellij.java"))

  instrumentCode.set(false)
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
  jvmToolchain(17)
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
    options.encoding = "UTF-8"
  }
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
  }

  patchPluginXml {
    sinceBuild.set("241") // Action Browser doesn't support 223
    untilBuild.set("242.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}

tasks {
  runIde {
    // Enable Hot Reload
    jvmArgs = listOf("-XX:+AllowEnhancedClassRedefinition -Dapple.laf.useScreenMenuBar=false -DjbScreenMenuBar.enabled=true")
  }
}

// https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin-faq.html#how-to-disable-building-searchable-options
tasks.buildSearchableOptions {
  enabled = false
}

buildscript {
  repositories {
//        mavenCentral()
    maven(url ="https://maven.aliyun.com/repository/public" )
    google()
  }
}

// Configure project's dependencies
repositories {
  maven(url = "https://maven.aliyun.com/repository/public")
//    mavenCentral()
  maven(url = "https://www.jetbrains.com/intellij-repository/releases")
  maven(url = "https://www.jetbrains.com/intellij-repository/snapshots")
  maven(url = "https://maven-central.storage-download.googleapis.com/repos/central/data/")
  maven(url = "https://repo.eclipse.org/content/groups/releases/")
}