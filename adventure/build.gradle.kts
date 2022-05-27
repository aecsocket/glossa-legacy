plugins {
    kotlin("jvm")
}

dependencies {
    api(projects.glossaCore)
    api(libs.adventureApi)
    implementation(libs.adventureTextMiniMessage)

    testImplementation(kotlin("test"))
    testImplementation(libs.adventureTextSerializerGson)
}
