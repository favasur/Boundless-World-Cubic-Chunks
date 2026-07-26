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
    implementation(project(":common"))
    // NeoForge's JAR-in-JAR loader pattern: the published mod jar will contain
    // :common's classes inline, so a single drop into .minecraft/mods is enough.
    jarJar(project(":common"))
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

// Drive `processResources` to substitute the literal `${version}` placeholder
// in META-INF/neoforge.mods.toml from `project.version`; without this FML
// rejects the published jar with `InvalidModFileException: Illegal version`.
// `inputs.property` registers `project.version` as a hashable task input so
// edits to `libs.versions.cubicchunks` invalidate this task's incremental
// cache automatically (no `--rerun-tasks` required).
tasks.processResources {
    inputs.property("cubicchunks.version", project.version.toString())
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version.toString())
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 21
}
