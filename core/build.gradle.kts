plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(libs.icu4j)

    testImplementation(kotlin("test"))
}
