rootProject.name = "cubicchunks"

include("common")
include("neoforge-1_21")
include("fabric-1_21")

// Future 1.26.x placeholders (stubs only)
include("neoforge-26")
include("fabric-26")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases/") {
            name = "NeoForge"
        }
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        maven("https://maven.spongepowered.org/repository/maven-public/") {
            name = "SpongePowered"
        }
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        mavenCentral()
    }
}
