enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version "1.6.21"
        id("org.jetbrains.dokka") version "1.6.21"
    }
}

rootProject.name = "glossa"

listOf(
    "core",
    "adventure",
    "configurate"
).forEach {
    val name = "${rootProject.name}-$it"
    include(name)
    project(":$name").apply {
        projectDir = file(it)
    }
}
