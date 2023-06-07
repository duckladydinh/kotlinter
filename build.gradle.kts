/**
 * To build the plugin for a particular .properties file, run the following command
 *
 * ```
 * ./gradlew signPlugin -Dconfig=IntelliJ221.properties -DminorVersion=$(git rev-parse --short HEAD)
 *
 * ```
 *
 * And check for the .zip plugin in the build/distribution directory.
 *
 * For development (with debugging enabled), we suggest setting the default config file and using
 * the IntelliJ Run Configuration to call `runIde`.
 */
import java.util.*

val configFile: String = System.getProperty("config", "IntelliJ223.properties")
val minorVersion: String = System.getProperty("minorVersion", "unversioned")
val config = Properties().apply { load(file("${rootProject.rootDir}/$configFile").inputStream()) }

val kotlinterMinorVersion = 2
val intellijJvmVersion: String = config.getProperty("intellij.jvm.version")
val intellijIdeVersion: String = config.getProperty("intellij.ide.version")
val intellijMinBuildVersion: String = config.getProperty("intellij.build.min.version")
val intellijMaxBuildVersion: String = config.getProperty("intellij.build.max.version")

plugins {
  id("java")
  kotlin("jvm") version "1.8.10"
  id("org.jetbrains.intellij") version "1.14.0"
}

group = "com.giathuan"

version = "1.$intellijIdeVersion.$minorVersion"

repositories { mavenCentral() }

dependencies { testImplementation(kotlin("test")) }

java { sourceCompatibility = JavaVersion.toVersion(intellijJvmVersion.toInt()) }

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
  version.set(intellijIdeVersion)
  plugins.set(
      listOf(
        "com.intellij.java",
        "org.jetbrains.kotlin",
        "com.google.idea.bazel.ijwb:2023.05.16.0.1-api-version-223",
      ))
}

tasks {
  runIde { autoReloadPlugins.set(true) }

  buildSearchableOptions { enabled = false }

  patchPluginXml {
    version.set("${project.version}")
    sinceBuild.set(intellijMinBuildVersion)
    untilBuild.set(intellijMaxBuildVersion)
  }

  publishPlugin { token.set(System.getenv("ORG_GRADLE_PROJECT_intellijPublishToken")) }

  compileKotlin { kotlinOptions.jvmTarget = intellijJvmVersion }

  compileTestKotlin { kotlinOptions.jvmTarget = intellijJvmVersion }
}
