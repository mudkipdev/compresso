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
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject) {
        version("26.1.2-fabric", "26.1.2")
        vcsVersion = "26.1.2-fabric"
    }
}

rootProject.name = "compresso"