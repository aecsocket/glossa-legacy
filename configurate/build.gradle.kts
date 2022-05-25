plugins {
    kotlin("jvm")
}

dependencies {
    implementation(projects.glossaCore)
    implementation(projects.glossaAdventure)
    implementation(libs.bundles.configurate)
    compileOnly(libs.bundles.adventure)

    testImplementation(kotlin("test"))
    testImplementation(libs.configurateHocon)
}
