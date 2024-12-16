plugins {
    id("asp.base-conventions")
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":aspaper-api"))
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "com.infernalsuite.asp.importer.ASPImporter"
        }
    }
    shadowJar {
        minimize()
    }
}

description = "asp-importer"
