plugins {
    base
}

allprojects {
    group = "io.github.opencubicchunks"
    version = "0.0.0-placeholder"

    repositories {
        // mavenLocal lets the platform modules consume :common as a published
        // artifact via `io.github.opencubicchunks:cubicchunks-common`, dodging
        // Loom 1.9-SNAPSHOT's config-phase cross-project JAR read that throws
        // NoSuchFileException on a clean build. Build sequence:
        //   ./gradlew --configure-on-demand --no-daemon --no-configuration-cache \
        //       :common:publishMavenJavaPublicationToMavenLocal
        //   ./gradlew clean build -x test --no-daemon --no-configuration-cache
        mavenLocal()
        mavenCentral()
    }
}

// Aggregate tasks could be added here (e.g., publish, test)
tasks.named<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
