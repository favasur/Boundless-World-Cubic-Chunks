plugins {
    java
    id("net.neoforged.moddev") version "1.0.21"
}

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

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 21
}
