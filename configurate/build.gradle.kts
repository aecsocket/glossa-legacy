plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(projects.glossaAdventure)
    compileOnly(libs.adventureApi)
    api(libs.configurateCore)
    implementation(libs.configurateExtraKotlin)
    implementation(libs.adventureSerializerConfigurate)

    testImplementation(kotlin("test"))
}
