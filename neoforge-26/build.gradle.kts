// Forward-compatible module for NeoForge 26.x.
// Currently mirrors the 1.21.x implementation using placeholder versions.
// The 26.x coordinates in libs.versions.toml (neoforge26) currently alias
// 21.1.54 because NeoForge 26.x is not yet released. When 26.x ships, swap
// the alias to the actual NeoForge 26.x version.

plugins {
    java
    id("net.neoforged.moddev") version "1.0.21"
}

// The 26.x toml uses `${version}` (this file). The moddev plugin substitutes
// it from `project.version`; if we don't declare one, the literal
// `${version}` ends up in the jar and FML throws `InvalidModFileException`.
// `libs.versions.cubicchunks` is the single source of truth shared with
// neoforge-1_21 so the two subprojects can't drift.
version = libs.versions.cubicchunks.get()

dependencies {
    // :common is built by Fabric Loom, which defaults its primary artifact to
    // intermediary-mapped names. NeoForge runs on Mojang official mappings, so
    // we MUST consume the "namedElements" configuration or the JIJ'd classes
    // will throw NoClassDefFoundError at runtime.
    implementation(project(":common", configuration = "namedElements"))
    // JAR-in-JAR mirror of neoforge-1_21 — the published mod jar will contain
    // :common's classes inline, so a single drop into .minecraft/mods is enough.
    jarJar(project(":common", configuration = "namedElements"))
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
// 26.x builds are opt-in via `-Pbuild.26x=true` or `BUILD_26X=true`. When the
// opt-in is absent we disable every task in this module (not just
// `build`/`jar`/`assemble`) because the NeoForge moddev plugin's `modDev`
// task set still configures RemapJar / sourcesJar / etc. and reads
// `build/devlibs/...` paths the same way Loom does. When the opt-in IS
// present, we register the same mod-metadata substitution wiring as
// neoforge-1_21 so the toml's `${version}` placeholder expands from
// `project.version` at packaging time instead of being shipped as a
// literal that FML rejects. `inputs.property` registers `project.version`
// as a hashable task input so edits to `libs.versions.cubicchunks`
// invalidate this task's incremental cache automatically (no
// `--rerun-tasks` required).
val build26x: Boolean =
    (project.findProperty("build.26x") as String?)?.toBoolean() == true ||
        System.getenv("BUILD_26X")?.toBoolean() == true
if (!build26x) {
    tasks.configureEach {
        enabled = false
    }
} else {
    tasks.processResources {
        inputs.property("cubicchunks.version", project.version.toString())
        filesMatching("META-INF/neoforge.mods.toml") {
            expand("version" to project.version.toString())
        }
        from(project(":common").sourceSets.main.get().resources.srcDirs) {
            include("cubicchunks*.json")
        }
    }
}
