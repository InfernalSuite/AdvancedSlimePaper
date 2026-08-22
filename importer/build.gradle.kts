plugins {
    id("asp.base-conventions")
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":aspaper-api"))
    implementation(libs.lz4)
    runtimeOnly(libs.slf4j.simple)
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
            exclude(dependency("org.slf4j:.*"))
        }
    }
    assemble {
        dependsOn(shadowJar)
    }
}

description = "asp-importer"
