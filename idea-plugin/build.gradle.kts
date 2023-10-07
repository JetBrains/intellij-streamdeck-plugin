plugins {
  id("java")
  id("org.jetbrains.intellij") version "1.13.3"
  id("org.jetbrains.kotlin.jvm") version "1.7.21"
}

group = "com.jetbrains.ide.streamdeck"
version = "2023.2.2"

repositories {
  mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
//  version.set("223.8214.52")
  version.set("232.9921.47")

  type.set("IC") // Target IDE Platform

  plugins.set(listOf("org.intellij.plugins.markdown"))

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
  }

  patchPluginXml {
    sinceBuild.set("231") // Action Browser doesn't support 223
    untilBuild.set("233.*")
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

// https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin-faq.html#how-to-disable-building-searchable-options
tasks.buildSearchableOptions {
  enabled = false
}