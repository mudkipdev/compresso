plugins {
    id("net.fabricmc.fabric-loom") version "1.16-SNAPSHOT"
}

val javaVersion = 25

base {
    archivesName = "compresso"
}

group = "dev.mudkip"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.isxander.dev/releases")

    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven")
        }

        filter {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:26.1.2")
    implementation("net.fabricmc:fabric-loader:0.19.2")
    implementation("net.fabricmc.fabric-api:fabric-api:0.146.1+26.1.2")
    implementation("maven.modrinth:yacl:3.9.4+26.1-fabric")
    implementation("com.terraformersmc:modmenu:18.0.0-alpha.8")
    include(implementation("com.github.usefulness:webp-imageio:0.11.0")!!)
}

loom {
    runConfigs.configureEach {
        ideConfigGenerated(true)
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }

    withSourcesJar()
}

tasks.processResources {
    val properties = mapOf(
        "version" to project.version,
        "loader_version" to "0.18.4",
        "jdk_version" to javaVersion
    )

    inputs.properties(properties)

    filesMatching("fabric.mod.json") {
        expand(properties)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = javaVersion
}

tasks.jar {
    from(rootProject.file("LICENSE")) {
        rename {
            "${it}_${base.archivesName.get()}" }
    }
}
