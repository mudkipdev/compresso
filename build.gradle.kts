plugins {
    id("fabric-loom") version "1.16-SNAPSHOT"
}

val obfuscated = !(findProperty("fabric.loom.disableObfuscation")?.toString()?.toBoolean() ?: false)
val javaVersion = (property("mod.java_version") as String).toInt()
val minecraftDependency = property("mod.minecraft_dependency") as String

base {
    archivesName = "compresso"
}

group = "dev.mudkip"
version = "1.0.1+${stonecutter.current.version}-fabric"

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
