plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1.2-fabric"

tasks.register("buildAllVersions") {
    group = "build"
    description = "Builds the mod for every Minecraft version"
    dependsOn(stonecutter.tasks.named("build").map { it.values })
}