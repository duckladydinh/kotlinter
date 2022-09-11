plugins {
  id("java")
  kotlin("jvm") version "1.6.10"
  id("org.jetbrains.intellij") version "1.9.0"
}

group = "com.giathuan"

version = "1.0"

repositories { mavenCentral() }

dependencies { testImplementation(kotlin("test")) }

java { sourceCompatibility = JavaVersion.VERSION_11 }

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
  version.set("2021.3.3")
  plugins.set(
      listOf(
          "com.intellij.java",
          "org.jetbrains.kotlin",
          // "com.google.idea.bazel.ijwb:2022.06.28.0.0-api-version-213",
          ))
}

tasks {
  runIde { autoReloadPlugins.set(true) }

  buildSearchableOptions { enabled = false }

  patchPluginXml {
    version.set("${project.version}")
    sinceBuild.set("213")
    untilBuild.set("213.*")
  }

  publishPlugin { token.set(System.getenv("ORG_GRADLE_PROJECT_intellijPublishToken")) }

  compileKotlin { kotlinOptions.jvmTarget = "11" }

  compileTestKotlin { kotlinOptions.jvmTarget = "11" }
}
