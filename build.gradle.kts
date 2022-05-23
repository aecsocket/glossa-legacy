plugins {
    kotlin("jvm")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

allprojects {
    group = "com.github.aecsocket.glossa"
    version = "0.3.0-SNAPSHOT"
    description = "ICU-based localization library"
}

subprojects {
    apply<JavaLibraryPlugin>()
    plugins.apply("org.jetbrains.dokka")
    plugins.apply("maven-publish")

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
}
