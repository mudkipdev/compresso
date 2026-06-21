import me.modmuss50.mpp.ReleaseType
import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    id("fabric-loom") version "1.16-SNAPSHOT"
    id("me.modmuss50.mod-publish-plugin") version "2.0.0"
}

val obfuscated = !(findProperty("fabric.loom.disableObfuscation")?.toString()?.toBoolean() ?: false)
val javaVersion = (property("mod.java_version") as String).toInt()
val minecraftDependency = property("mod.minecraft_dependency") as String

base {
    archivesName = "compresso"
}

group = "dev.mudkip"
version = "${property("mod.version")}+${stonecutter.current.version}-fabric"

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

val modConfiguration = if (obfuscated) "modImplementation" else "implementation"

dependencies {
    minecraft("com.mojang:minecraft:${stonecutter.current.version}")

    if (obfuscated) {
        "mappings"(loom.officialMojangMappings())
    }

    add(modConfiguration, "net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    add(modConfiguration, "net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    add(modConfiguration, property("deps.yacl") as String)
    add(modConfiguration, "com.terraformersmc:modmenu:${property("deps.modmenu")}")
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
        "jdk_version" to javaVersion,
        "minecraft_dependency" to minecraftDependency
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
        rename { "${it}_compresso" }
    }
}

val publishJar = if (obfuscated) tasks.named<AbstractArchiveTask>("remapJar") else tasks.named<AbstractArchiveTask>("jar")
val publishSourcesJar = if (obfuscated) tasks.named<AbstractArchiveTask>("remapSourcesJar") else tasks.named<AbstractArchiveTask>("sourcesJar")

publishMods {
    file = publishJar.flatMap { it.archiveFile }
    additionalFiles.from(publishSourcesJar.flatMap { it.archiveFile })
    displayName = "Compresso ${project.version}"
    version = project.version.toString()
    type = providers.environmentVariable("RELEASE_TYPE").map { ReleaseType.valueOf(it) }.orElse(ReleaseType.STABLE)
    changelog = providers.environmentVariable("CHANGELOG").orElse("No changelog provided.")
    modLoaders.add("fabric")

    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        projectId = "compresso"
        minecraftVersions.add(stonecutter.current.version)
        requires("fabric-api", "yacl")
        optional("modmenu")
    }
}
