<div align="center">

<!--<a href="https://aecsocket.gitlab.io/glossa"><img src="banner.png" width="1024" alt="Glossa banner" /></a>-->

# [Glossa](https://aecsocket.gitlab.io/glossa)

`0.0.1-SNAPSHOT`:
[![build](https://github.com/aecsocket/glossa/actions/workflows/build.yml/badge.svg)](https://github.com/aecsocket/glossa/actions/workflows/build.yml)

</div>

Kotlin localization library, with support for Adventure styling.

Because there weren't enough localization libraries already.

# Usage

## Packages

Using any package from the GitHub Packages registry requires you to
authorize with GitHub Packages.

### To create a token for yourself:

1. Visit https://github.com/settings/tokens/new
2. Create a token with only the `read:packages` scope
3. Save that token as an environment variable, `GPR_TOKEN`
4. Save your GitHub username as another environment variable, `GPR_ACTOR`

### To use a token in a workflow run:

Include the `github.actor` and `secrets.GITHUB_TOKEN` variables in the `env` block of your step:

```yml
- name: "Build"
  run: ./gradlew build
  env:
    GPR_ACTOR: "${{ github.actor }}"
    GPR_TOKEN: "${{ secrets.GITHUB_TOKEN }}"
```

### To use the token in your environment variable:

Use the `GPR_ACTOR`, `GPR_TOKEN` environment variables in your build scripts:

```kotlin
// authenticating with the repository
credentials {
    username = System.getenv("GPR_ACTOR")
    password = System.getenv("GPR_TOKEN")
}
```

**Note: Never include your token directly in your build scripts!**

Always use an environment variable (or similar).

<details>
<summary>Maven</summary>

### [How to authorize](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)

#### In `~/.m2/settings.xml`

```xml
<servers>
  <server>
    <id>github-minecommons</id>
    <username>[username]</username>
    <password>[token]</password>
  </server>
</servers>
```

#### In `pom.xml`

Repository
```xml
<repositories>
  <repository>
    <id>github-minecommons</id>
    <url>https://maven.pkg.github.com/aecsocket/minecommons</url>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>
```

Dependency
```xml
<dependencies>
  <dependency>
    <groupId>com.github.aecsocket</groupId>
    <artifactId>minecommons-[module]</artifactId>
    <version>[version]</version>
  </dependency>
</dependencies>
```

</details>

<details>
<summary>Gradle</summary>

The Kotlin DSL is used here.

### [How to authorize](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)

Repository
```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/aecsocket/minecommons")
        credentials {
            username = System.getenv("GPR_ACTOR")
            password = System.getenv("GPR_TOKEN")
        }
    }
}
```

Dependency
```kotlin
dependencies {
    compileOnly("com.github.aecsocket", "minecommons-[module]", "[version]")
}
```

</details>

## Documentation

TODO probably do kotlin docs

### [Javadoc](https://aecsocket.github.io/glossa/docs)
