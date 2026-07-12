import me.modmuss50.mpp.ReleaseType
import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    id("dev.architectury.loom") version "1.17.491"
    id("me.modmuss50.mod-publish-plugin") version "2.0.0"
}

val loaderName = loom.platform.get().name.lowercase()
val isFabric = loaderName == "fabric"
val isNeoforge = loaderName == "neoforge"

val obfuscated = !(findProperty("fabric.loom.disableObfuscation")?.toString()?.toBoolean() ?: false)
val javaVersion = (property("mod.java_version") as String).toInt()
val minecraftDependency = property("mod.minecraft_dependency") as String

base {
    archivesName = "compresso"
}

group = "dev.mudkip"
version = "${property("mod.version")}+${stonecutter.current.version}-$loaderName"

stonecutter {
    constants {
        match(loaderName, "fabric", "neoforge")
    }
}

repositories {
    mavenCentral()
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.isxander.dev/releases")
    maven("https://maven.neoforged.net/releases")
    maven("https://thedarkcolour.github.io/KotlinForForge/")

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

    if (isFabric) {
        add(modConfiguration, "net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
        add(modConfiguration, "net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
        add(modConfiguration, property("deps.yacl") as String)
        add(modConfiguration, "com.terraformersmc:modmenu:${property("deps.modmenu")}")
    } else {
        "neoForge"("net.neoforged:neoforge:${property("deps.neoforge")}")
        modConfiguration(property("deps.yacl") as String)
    }

    include(implementation("com.github.usefulness:webp-imageio:0.11.0")!!)

    compileOnly("org.jspecify:jspecify:1.0.0")
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
        "minecraft_dependency" to minecraftDependency,
        "neoforge_dependency" to (findProperty("deps.neoforge_dependency") ?: "[0,)"),
        "yacl_dependency" to (findProperty("deps.yacl_dependency") ?: "*")
    )

    inputs.properties(properties)

    if (isFabric) {
        filesMatching("fabric.mod.json") {
            expand(properties)
        }
        exclude("META-INF/neoforge.mods.toml")
    } else {
        filesMatching("META-INF/neoforge.mods.toml") {
            expand(properties)
        }
        exclude("fabric.mod.json")
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
    modLoaders.add(loaderName)

    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        projectId = "compresso"
        minecraftVersions.add(stonecutter.current.version)
        requires("yacl")

        if (isFabric) {
            requires("fabric-api")
            optional("modmenu")
        }
    }
}
