plugins {
    kotlin("jvm")
}

dependencies {
    implementation(projects.glossaApi)
    implementation(libs.bundles.configurate)
    compileOnly(libs.bundles.adventure)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.adventure)
    testImplementation(libs.adventureTextSerializerGson)
    testImplementation(libs.adventureTextSerializerPlain)
}
