plugins {
    // Loom 1.9-SNAPSHOT is the only stable stream published for MC 1.21.x; the
    // historic v37/v42 green builds used a transient snapshot that has since
    // regressed. The build workaround is to publish :common to mavenLocal and
    // reference it as a mavenLocal coordinate (see fabric-1_21/build.gradle.kts).
    id("fabric-loom") version "1.9-SNAPSHOT"
    `java-library`
    `maven-publish`
}

repositories {
    maven("https://maven.fabricmc.net/") {
        name = "Fabric"
    }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.1")
    mappings(loom.officialMojangMappings())
    implementation("net.fabricmc:fabric-loader:0.16.9")
    annotationProcessor("net.fabricmc:sponge-mixin:0.15.3+mixin.0.8.7")
    implementation("net.fabricmc:sponge-mixin:0.15.3+mixin.0.8.7")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
}

publishing {
    publications {
        // Publication name is the identifier Gradle uses to name the generated
        // tasks (publishMavenJavaPublicationToMavenLocal etc.). It is NOT the
        // repository name — we publish to mavenLocal() regardless of the
        // publication name.
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "io.github.opencubicchunks"
            artifactId = "cubicchunks-common"
            // Maven coord version is the same as the modId version in
            // common/src/main/resources/fabric.mod.json ("0.4"). Maven
            // coordinates are free-form so we mirror the semver-valid "0.4"
            // string the platforms depend on via
            // fabric.mod.json's depends.cubicchunks_common: ">=0.4".
            version = "0.4"
        }
    }
    repositories {
        mavenLocal()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
