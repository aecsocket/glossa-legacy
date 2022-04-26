plugins {
    kotlin("jvm")
    id("maven-publish")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.adventure)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/aecsocket/glossa")
            credentials {
                username = System.getenv("GPR_ACTOR")
                password = System.getenv("GPR_TOKEN")
            }
        }
    }
}
