// Forward-compatible module for NeoForge 26.x.
// Currently mirrors the 1.21.x implementation using placeholder versions.
// Update gradle/libs.versions.toml and this file when NeoForge 26.x tooling is released.

plugins {
    java
    id("net.neoforged.moddev") version "1.0.21"
}

dependencies {
    implementation(project(":common"))
}

neoForge {
    // TODO: replace with actual NeoForge 26.x version once available
    version = libs.versions.neoforge121.get()

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
