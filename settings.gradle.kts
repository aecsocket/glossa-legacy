enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version "1.8.0"
        id("org.jetbrains.dokka") version "1.7.20"
    }
}

rootProject.name = "glossa"

listOf(
    "core",
    "adventure",
).forEach {
    val name = "${rootProject.name}-$it"
    include(name)
    project(":$name").apply {
        projectDir = file(it)
    }
}
