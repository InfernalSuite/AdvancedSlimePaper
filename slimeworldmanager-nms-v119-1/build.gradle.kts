plugins {
    id("io.papermc.paperweight.userdev") version "1.3.5"
}

dependencies {
    paperDevBundle("1.19.1-R0.1-SNAPSHOT")

    compileOnly(project(":slimeworldmanager-nms-common"))
    compileOnly(project(":slimeworldmanager-api"))
    compileOnly(project(":slimeworldmanager-classmodifierapi"))

    implementation("com.flowpowered:flow-nbt:2.0.2")
}


tasks {
    assemble {
        dependsOn(reobfJar)
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    compileJava {
        options.release.set(17)
    }

}

description = "slimeworldmanager-nms-v119-1"
