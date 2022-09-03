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

    testImplementation(platform("org.junit:junit-bom:5.9.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.adventureApi)
    testImplementation(libs.adventureTextSerializerGson)
    testImplementation(libs.adventureTextMiniMessage)
    testImplementation(libs.adventureSerializerConfigurate)
    testImplementation(libs.configurateHocon)
}
