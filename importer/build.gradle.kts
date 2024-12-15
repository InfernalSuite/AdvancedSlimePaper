plugins {
    id("com.gradleup.shadow") version "8.3.5"
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
