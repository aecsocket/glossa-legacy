enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }

    plugins {
        kotlin("jvm") version "1.6.21"
        id("io.papermc.paperweight.userdev") version "1.3.5"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "glossa"

subproject("${rootProject.name}-api") {
    projectDir = file("api")
}
subproject("${rootProject.name}-adventure") {
    projectDir = file("adventure")
}

inline fun subproject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}
