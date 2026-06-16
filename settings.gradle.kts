pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net")
        maven("https://maven.kikugie.dev/releases")
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.6"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject) {
        version("1.21.11-fabric", "1.21.11")
        version("26.1.2-fabric", "26.1.2")
        version("26.2-fabric", "26.2")
        vcsVersion = "26.1.2-fabric"
    }
}

rootProject.name = "compresso"