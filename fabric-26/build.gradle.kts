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
    // 26.x coordinates: currently aliased to 1.21.x in libs.versions.toml;
    // swap to the actual 1.26.x coordinate when Mojang releases it.
    minecraft(libs.minecraft26)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabricLoader26)
    modImplementation(libs.fabricApi26)

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

// Opt-in: 26.x modules don't build by default because 1.26.x is not yet
// released. Enable with `-Pbuild.26x=true` on the CLI or `BUILD_26X=true`
// in the env. The 26.x maven coordinates in libs.versions.toml currently
// alias to 1.21.x so the build is a stub that proves the 26.x toolchain
// works; when 1.26.x ships, swap the aliases to the real coordinates.
//
// We disable EVERY task in this module when the opt-in is absent (not just
// `build`/`jar`/`assemble`) because Loom's `remapJar` task reads
// `build/devlibs/<artifact>-dev.jar` regardless of whether `:build` is
// requested, and that fails when the module has never been compiled.
val build26x: Boolean =
    (project.findProperty("build.26x") as String?)?.toBoolean() == true ||
        System.getenv("BUILD_26X")?.toBoolean() == true
if (!build26x) {
    tasks.configureEach {
        enabled = false
    }
}
