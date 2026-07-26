// Forward-compatible module for Fabric 1.26.x.
// Currently mirrors the 1.21.x implementation using placeholder versions.
// Update gradle/libs.versions.toml and this file when Fabric 1.26.x tooling is released.

plugins {
    java
    id("fabric-loom") version "1.9-SNAPSHOT"
}

dependencies {
    minecraft(libs.minecraft121)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabricLoader121)
    modImplementation(libs.fabricApi121)

    implementation(project(":common"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 21
}
