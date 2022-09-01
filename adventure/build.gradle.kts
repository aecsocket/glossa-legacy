plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    api(projects.glossaCore)
    implementation(libs.icu4j)
    compileOnly(libs.adventureApi)
    compileOnly(libs.adventureTextMiniMessage)
    compileOnly(libs.adventureSerializerConfigurate)
    compileOnly(libs.configurateCore)
    compileOnly(libs.configurateExtraKotlin)

    testImplementation(kotlin("test"))
    testImplementation(libs.adventureApi)
    testImplementation(libs.adventureTextSerializerGson)
    testImplementation(libs.adventureTextMiniMessage)
    testImplementation(libs.adventureSerializerConfigurate)
    testImplementation(libs.configurateHocon)
}
