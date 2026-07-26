plugins {
    java
    // Pinned to loom 1.9.4 — match common/build.gradle.kts to keep cross-project
    // eager-resolution semantics consistent across modules.
    id("fabric-loom") version "1.9-SNAPSHOT"
}

dependencies {
    minecraft(libs.minecraft121)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabricLoader121)
    modImplementation(libs.fabricApi121)

    // Fabric Loom 1.9+: modImplementation nests the :common classes INSIDE the
    // published mod jar so vanilla MultiMC/Prism/.minecraft can drop a single jar
    // into the mods folder. include() additionally merges common's resources
    // (cubicchunks.mixins.json, common-refmap.json) into the mod jar so the mixin
    // loader picks them up at runtime.
    modImplementation(project(":common"))
    include(project(":common"))
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
