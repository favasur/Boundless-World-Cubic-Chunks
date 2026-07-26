plugins {
    base
}

allprojects {
    group = "io.github.opencubicchunks"
    version = "0.0.0-placeholder"

    repositories {
        mavenCentral()
    }
}

// Aggregate tasks could be added here (e.g., publish, test)
tasks.named<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
