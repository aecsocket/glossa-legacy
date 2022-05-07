<div align="center">

<!--<a href="https://aecsocket.gitlab.io/glossa"><img src="banner.png" width="1024" alt="Glossa banner" /></a>-->

# [Glossa](https://aecsocket.gitlab.io/glossa)

`0.1.0-SNAPSHOT`:
[![build](https://github.com/aecsocket/glossa/actions/workflows/build.yml/badge.svg)](https://github.com/aecsocket/glossa/actions/workflows/build.yml)

</div>

ICU-based localization library, written in Kotlin, with support for Adventure styling.

Because there weren't enough localization libraries already.

# Usage

## Dependency

`gradle/libs.version.toml`
```toml
[versions]
glossa = "main-SNAPSHOT"

[libraries]
glossa = { group = "com.github.aecsocket", name = "glossa-[MODULE]", version.ref = "glossa" }
```

`build.gradle.kts`
```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation(libs.glossa)
}
```

## Modules

* `glossa-api` Core API classes, including `StringI18N`
* `glossa-adventure` [Adventure](https://github.com/kyoriPowered/adventure) support

## Documentation

### [Dokka](https://aecsocket.github.io/glossa/docs)
