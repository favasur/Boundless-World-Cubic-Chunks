plugins {
    java
    id("fabric-loom") version "1.9-SNAPSHOT"
}

dependencies {
    minecraft(libs.minecraft121)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabricLoader121)
    modImplementation(libs.fabricApi121)

    implementation(project(":common"))
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
