plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(projects.glossaAdventure)
    implementation(libs.adventureSerializerConfigurate)
    api(libs.configurateCore)
    implementation(libs.configurateExtraKotlin)
    implementation(libs.alexandriaCore)

    testImplementation(kotlin("test"))
}
