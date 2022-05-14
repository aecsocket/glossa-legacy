plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.icu4j)
    implementation(libs.bundles.configurate)

    testImplementation(kotlin("test"))
    testImplementation(libs.configurateHocon)
}
