plugins {
    kotlin("jvm")
    id("java-library")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

allprojects {
    group = "com.github.aecsocket"
    version = "0.1.0-SNAPSHOT"
    description = "ICU-based localization library"
}

subprojects {
    apply<JavaLibraryPlugin>()
    plugins.apply("org.jetbrains.dokka")
    plugins.apply("maven-publish")

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        withSourcesJar()
        withJavadocJar()
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
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
