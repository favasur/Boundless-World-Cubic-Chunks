// Forward-compatible module for Fabric 1.26.x.
// Currently mirrors the 1.21.x implementation using placeholder versions.
// Update gradle/libs.versions.toml and this file when Fabric 1.26.x tooling is released.

plugins {
    java
    // Loom 1.9-SNAPSHOT is the only stable stream for Minecraft 1.21.x.
    // 1.9.4 / 1.9.0 / 1.17 stable releases referenced in older comments do
    // not exist on Maven Central. See root build.gradle.kts for the
    // mavenLocal cross-module workaround that lets the build succeed.
    id("fabric-loom") version "1.9-SNAPSHOT"
}

dependencies {
    minecraft(libs.minecraft121)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabricLoader121)
    modImplementation(libs.fabricApi121)

    // :common is a mavenLocal dep (not project(":common")) for the same reason
    // documented in fabric-1_21/build.gradle.kts — avoids Loom 1.9-SNAPSHOT's
    // config-phase cross-project JAR read. Build sequence:
    // ./gradlew --configure-on-demand --no-daemon --no-configuration-cache :common:publishMavenJavaPublicationToMavenLocal
    // then ./gradlew clean build -x test --no-daemon --no-configuration-cache.
    modImplementation("io.github.opencubicchunks:cubicchunks-common:0.4")
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
