plugins {
    id("asp.base-conventions")
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":aspaper-api"))
    implementation(libs.lz4)
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "com.infernalsuite.asp.importer.SWMImporter"
        }
    }
    shadowJar {
        archiveClassifier.set("")
        minimize {
            exclude(dependency("at.yawk.lz4:.*"))
        }
    }
    assemble {
        dependsOn(shadowJar)
    }
}

description = "asp-importer"
