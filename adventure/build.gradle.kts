plugins {
    kotlin("jvm")
}

dependencies {
    implementation(projects.glossaApi)
    compileOnly(libs.bundles.adventure)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.adventure)
    testImplementation(libs.adventureTextSerializerGson)
    testImplementation(libs.adventureTextSerializerPlain)
}
