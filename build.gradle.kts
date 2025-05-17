/**
 * To build the plugin for a particular .properties file, run the following command
 *
 * ```
 * ./gradlew signPlugin -Dconfig=IntelliJ243.properties -DminorVersion=$(git rev-parse --short HEAD)
 *
 * ```
 *
 * And check for the .zip plugin in the build/distribution directory.
 *
 * For development (with debugging enabled), we suggest setting the default config file and using
 * the IntelliJ Run Configuration to call `runIde`.
 */
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import java.util.*

val configFile: String = System.getProperty("config", "IntelliJ243.properties")
val minorVersion: String = System.getProperty("minorVersion", "unversioned")
val config = Properties().apply { load(file("${rootProject.rootDir}/$configFile").inputStream()) }

val intellijJvmVersion: String = config.getProperty("intellij.jvm.version")
val intellijJavaVersion: JavaVersion = JavaVersion.toVersion(intellijJvmVersion.toInt())
val intellijKotlinVersion: Int = intellijJvmVersion.toInt()
val intellijIdeVersion: String = config.getProperty("intellij.ide.version")
val intellijMinBuildVersion: String = config.getProperty("intellij.build.min.version")

version = "1.$intellijMinBuildVersion.$intellijIdeVersion.$minorVersion"

plugins {
  id("java")
  kotlin("jvm") version "2.1.20"
  id("org.jetbrains.intellij.platform") version "2.5.0"
}

repositories {
  mavenCentral()

  intellijPlatform { defaultRepositories() }
}

dependencies {
  intellijPlatform {
    intellijIdeaCommunity(intellijIdeVersion)
    // androidStudio("2024.3.1.13")
    bundledPlugin("org.jetbrains.kotlin")
    // Only for debugging:
    // plugin("com.facebook.ktfmt_idea_plugin:1.2.0.54")
    // plugin("com.google.idea.bazel.ijwb:2025.02.18.0.1-api-version-243")
    pluginVerifier()
    zipSigner()
    testFramework(TestFrameworkType.Platform)
  }
  testImplementation(kotlin("test"))
}

java {
  sourceCompatibility = intellijJavaVersion
  targetCompatibility = intellijJavaVersion
}

kotlin {
  jvmToolchain(intellijKotlinVersion)

  compilerOptions { freeCompilerArgs.add("-Xcontext-parameters") }
}

intellijPlatform {
  pluginConfiguration {
    name = "Kotlinter"
    ideaVersion {
      sinceBuild = intellijMinBuildVersion
      untilBuild = provider { null }
    }
  }
  buildSearchableOptions = false
}

tasks {
  // If there's PERMISSION_DENIED, run:
  // $ chmod 777 -R /home/thuan/IdeaProjects/kotlinter/build/idea-sandbox/
  runIde {
    autoReload = true
    jvmArgumentProviders += CommandLineArgumentProvider {
      listOf("-Didea.kotlin.plugin.use.k2=true")
    }
  }
  publishPlugin { token.set(providers.environmentVariable("PUBLISH_TOKEN")) }
}
