plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "com.infernalsuite.aswm.importer.SWMImporter"
        }
    }
}

description = "slimeworldmanager-importer"
