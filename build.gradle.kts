plugins {
    kotlin("jvm")
    id("java-library")
    id("maven-publish")
}

allprojects {
    group = "com.github.aecsocket"
    version = "0.0.1-SNAPSHOT"
    description = "Simple localization library"
}

subprojects {
    apply<JavaLibraryPlugin>()

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        withSourcesJar()
        withJavadocJar()
    }

    tasks {
        compileJava {
            options.encoding = Charsets.UTF_8.name()
        }

        test {
            useJUnitPlatform()
        }
    }
}
