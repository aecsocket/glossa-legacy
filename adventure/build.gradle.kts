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
    implementation(libs.adventureTextMiniMessage)

    testImplementation(kotlin("test"))
    testImplementation(libs.adventureTextSerializerGson)
}
