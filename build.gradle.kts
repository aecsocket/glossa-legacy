plugins {
    kotlin("jvm")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

allprojects {
    group = "com.gitlab.aecsocket.glossa"
    version = "0.3.6"
    description = "ICU-based localization library"
}

repositories {
    mavenLocal()
    mavenCentral()
}

subprojects {
    apply<JavaLibraryPlugin>()
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")

    tasks {
        test {
            useJUnitPlatform()
        }
    }

    publishing {
        repositories {
            maven {
                url = uri("https://gitlab.com/api/v4/groups/phosphorous/-/packages/maven")
                credentials(HttpHeaderCredentials::class) {
                    name = "Job-Token"
                    value = System.getenv("CI_JOB_TOKEN")
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }

        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}
