plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
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