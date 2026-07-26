// Forward-compatible module for NeoForge 26.x.
// Currently mirrors the 1.21.x implementation using placeholder versions.
// The 26.x coordinates in libs.versions.toml (neoforge26) currently alias
// 21.1.54 because NeoForge 26.x is not yet released. When 26.x ships, swap
// the alias to the actual NeoForge 26.x version.

plugins {
    java
    id("net.neoforged.moddev") version "1.0.21"
}

dependencies {
    implementation(project(":common"))
    // JAR-in-JAR mirror of neoforge-1_21 — the published mod jar will contain
    // :common's classes inline, so a single drop into .minecraft/mods is enough.
    jarJar(project(":common"))
}

neoForge {
    version = libs.versions.neoforge26.get()

    mods {
        create("cubicchunks") {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        create("client") { client() }
        create("server") { server() }
        create("data") { data() }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
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
// `build`/`jar`/`assemble`) because the NeoForge moddev plugin's `modDev`
// task set still configures RemapJar / sourcesJar / etc. and reads
// `build/devlibs/...` paths the same way Loom does.
val build26x: Boolean =
    (project.findProperty("build.26x") as String?)?.toBoolean() == true ||
        System.getenv("BUILD_26X")?.toBoolean() == true
if (!build26x) {
    tasks.configureEach {
        enabled = false
    }
}
