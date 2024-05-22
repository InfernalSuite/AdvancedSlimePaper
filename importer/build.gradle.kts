plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":slimeworldmanager-api"))
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "com.infernalsuite.aswm.importer.SWMImporter"
        }
    }
    shadowJar {
        minimize()
    }
}

description = "slimeworldmanager-importer"
