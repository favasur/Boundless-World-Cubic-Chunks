rootProject.name = "cubicchunks"

include("common")
include("neoforge-1_21")
include("fabric-1_21")

// Future 1.26.x forward-stubs. Maven coordinates in `gradle/libs.versions.toml`
// (minecraft26 / neoforge26 / fabricLoader26 / fabricApi26) currently alias
// 1.21.x because 1.26.x is not yet released. The 26.x modules are opt-in:
// their build/jar tasks are gated on the `-Pbuild.26x=true` project property
// (or `BUILD_26X=true` env var). Default `./gradlew clean build` skips them;
// `./gradlew -Pbuild.26x=true clean build` produces all 5 jars.
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
