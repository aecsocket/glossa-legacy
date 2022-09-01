plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(libs.icu4j)
    compileOnly(libs.configurateCore)
    compileOnly(libs.configurateExtraKotlin)

    testImplementation(kotlin("test"))
}
