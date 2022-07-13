plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    api(projects.glossaAdventure)

    compileOnly(libs.adventureApi)
    compileOnly(libs.configurateCore)
    implementation(libs.configurateExtraKotlin)
    implementation(libs.adventureSerializerConfigurate)

    testImplementation(kotlin("test"))
}
