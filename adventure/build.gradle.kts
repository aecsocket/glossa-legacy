plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    api(projects.glossaCore)
    compileOnly(libs.adventureApi)
    compileOnly(libs.adventureTextMiniMessage)

    testImplementation(kotlin("test"))
    testImplementation(libs.adventureApi)
    testImplementation(libs.adventureTextSerializerGson)
    testImplementation(libs.adventureTextMiniMessage)
}
