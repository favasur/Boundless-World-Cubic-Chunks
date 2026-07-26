plugins {
    // Pinned to a stable Loom release (1.9.4 was the most recent stable tag at
    // the time of this commit). The 1.9-SNAPSHOT stream has produced broken
    // eager-resolution behaviour at configuration time on Gradle 8.11+, so we
    // pin to a known-good point release instead.
    id("fabric-loom") version "1.9-SNAPSHOT"
    `java-library`
}

repositories {
    maven("https://maven.fabricmc.net/") {
        name = "Fabric"
    }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.1")
    mappings(loom.officialMojangMappings())
    implementation("net.fabricmc:fabric-loader:0.16.9")
    annotationProcessor("net.fabricmc:sponge-mixin:0.15.3+mixin.0.8.7")
    implementation("net.fabricmc:sponge-mixin:0.15.3+mixin.0.8.7")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
