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

    // :common is referenced as a `io.github.opencubicchunks:cubicchunks-common`
    // mavenLocal coordinate rather than `project(":common")` to dodge Loom
    // 1.9-SNAPSHOT's eager cross-project JAR read at configuration time, which
    // throws NoSuchFileException on a clean build. Build sequence:
    // `./gradlew --configure-on-demand --no-daemon --no-configuration-cache :common:publishMavenJavaPublicationToMavenLocal`
    // (once per checkout or after any change to :common) followed by
    // `./gradlew clean build -x test --no-daemon --no-configuration-cache`. The
    // runtime link between the platform jar and common's classes is restored
    // via fabric.mod.json's `depends.cubicchunks_common` sibling-mod entry —
    // drop BOTH `common-*.jar` AND `fabric-1_21-*.jar` into `.minecraft/mods`.
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
