plugins {
    java
    id("net.neoforged.moddev") version "1.0.21"
}

// The toml at src/main/resources/META-INF/neoforge.mods.toml uses the
// `${version}` placeholder. The moddev plugin substitutes it from
// `project.version`. Without a value here, the literal `${version}` is
// shipped in the jar and FML throws InvalidModFileException.
// `libs.versions.cubicchunks` is the single source of truth shared with
// neoforge-26 so the two subprojects can't drift.
version = libs.versions.cubicchunks.get()

dependencies {
    // :common is built by Fabric Loom, which defaults its primary artifact to
    // intermediary-mapped names (net.minecraft.class_1937). NeoForge runs on
    // Mojang official mappings (net.minecraft.world.level.Level), so we MUST
    // consume the "namedElements" configuration — the Mojang-mapped jar — or
    // the JIJ'd classes will throw NoClassDefFoundError at runtime.
    implementation(project(":common", configuration = "namedElements"))

    // Instead of using jarJar (which creates a separate Java module that can't
    // read the minecraft module, causing IllegalAccessError), we merge the
    // common module's classes directly into the neoforge jar via from(). This
    // eliminates the module boundary — all classes load in the same module.
}

neoForge {
    version = libs.versions.neoforge121.get()

    mods {
        create("cubicchunks") {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
        }
        create("data") {
            data()
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Drive `processResources` to:
// 1. Substitute `${version}` in META-INF/neoforge.mods.toml from
//    `project.version` (FML rejects the literal placeholder).
// 2. Copy mixin config JSONs from :common into the neoforge jar. The
//    `[[mixins]]` declarations in neoforge.mods.toml reference these files,
//    and Mixin's classloader can't find them inside the JIJ'd common jar
//    (JIJ union filesystems don't expose resources to ClassLoader).
// `inputs.property` registers `project.version` as a hashable task input so
// edits to `libs.versions.cubicchunks` invalidate this task's incremental
// cache automatically (no `--rerun-tasks` required).
tasks.processResources {
    inputs.property("cubicchunks.version", project.version.toString())
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version.toString())
    }
    from(project(":common").sourceSets.main.get().resources.srcDirs) {
        include("cubicchunks*.json")
    }
}

// Merge :common classes directly into the neoforge jar instead of using JIJ.
// JIJ creates a separate Java module that can't read the minecraft module
// (IllegalAccessError: module cubicchunks.common does not read module
// minecraft). Merging eliminates the module boundary entirely.
val commonJar by configurations.creating
dependencies {
    commonJar(project(":common", configuration = "namedElements"))
}
tasks.jar {
    from(provider { commonJar.files.map { zipTree(it) } }) {
        exclude("META-INF/MANIFEST.MF")   // don't overwrite our manifest
        exclude("META-INF/mods.toml")     // don't overwrite neoforge.mods.toml
        exclude("META-INF/neoforge.mods.toml")
        exclude("fabric.mod.json")       // Fabric metadata not needed
        exclude("module-info.class")     // redundant — already excluded upstream
        exclude("cubicchunks*.json")     // already copied by processResources
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 21
}
