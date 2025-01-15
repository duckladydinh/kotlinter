/**
 * To build the plugin for a particular .properties file, run the following command
 *
 * ```
 * ./gradlew signPlugin -Dconfig=IntelliJ242.properties -DminorVersion=$(git rev-parse --short HEAD)
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

val configFile: String = System.getProperty("config", "IntelliJ242.properties")
val minorVersion: String = System.getProperty("minorVersion", "unversioned")
val config = Properties().apply { load(file("${rootProject.rootDir}/$configFile").inputStream()) }

val intellijJvmVersion: String = config.getProperty("intellij.jvm.version")
val intellijJavaVersion: JavaVersion = JavaVersion.toVersion(intellijJvmVersion.toInt())
val intellijIdeVersion: String = config.getProperty("intellij.ide.version")
val intellijMinBuildVersion: String = config.getProperty("intellij.build.min.version")

plugins {
  id("java")
  kotlin("jvm") version "2.1.0"
  id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "com.giathuan"

version = "1.$intellijIdeVersion.$minorVersion"

repositories {
  mavenCentral()

  intellijPlatform { defaultRepositories() }
}

dependencies {
  intellijPlatform {
    intellijIdeaCommunity(intellijIdeVersion)
    bundledPlugin("com.intellij.java")
    bundledPlugin("org.jetbrains.kotlin")
    pluginVerifier()
    zipSigner()
    testFramework(TestFrameworkType.Platform)
  }
  testImplementation(kotlin("test"))
}

java { sourceCompatibility = intellijJavaVersion }

intellijPlatform {
  pluginConfiguration {
    ideaVersion {
      sinceBuild = intellijMinBuildVersion
      untilBuild = provider { null }
    }
  }
  pluginVerification { ides { recommended() } }
}

tasks {
  runIde { autoReload = true }

  buildSearchableOptions { enabled = false }

  signPlugin {
    certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
    privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
    password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin { token.set(providers.environmentVariable("PUBLISH_TOKEN")) }
}
