plugins {
    kotlin("jvm")
}

dependencies {
    api(projects.glossaCore)
    implementation(libs.bundles.configurate)
    compileOnly(libs.bundles.adventure)

    testImplementation(kotlin("test"))
    testImplementation(libs.configurateHocon)
    testImplementation(libs.bundles.adventure)
    testImplementation(libs.adventureTextSerializerGson)
}
