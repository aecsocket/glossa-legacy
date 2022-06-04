plugins {
    kotlin("jvm")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

allprojects {
    group = "com.github.aecsocket.glossa"
    version = "0.3.3"
    description = "ICU-based localization library"
}

repositories {
    mavenLocal()
    mavenCentral()
}

subprojects {
    apply<JavaLibraryPlugin>()
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "maven-publish")

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }

    tasks {
        test {
            useJUnitPlatform()
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}
