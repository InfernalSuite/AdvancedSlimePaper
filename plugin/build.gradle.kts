plugins {
    id("com.gradleup.shadow") version "8.3.5"
    id("net.kyori.blossom") version "2.1.0"
}

version = "3.0.1"

sourceSets {
    main {
        blossom {
            resources {
                property("version", version.toString())
            }
        }
    }
}

dependencies {
    compileOnly(project(":api"))

    implementation(project(":loaders"))
    implementation("org.spongepowered:configurate-yaml:4.2.0")
    implementation("org.bstats:bstats-bukkit:3.0.2")
    implementation("org.incendo:cloud-paper:2.0.0-beta.10")
    implementation("org.incendo:cloud-minecraft-extras:2.0.0-beta.10")
    implementation("org.incendo:cloud-annotations:2.0.0")
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveClassifier.set("")

        relocate("org.bstats", "com.infernalsuite.aswm.internal.bstats")
        relocate("ninja.leaping.configurate", "com.infernalsuite.aswm.internal.configurate")
        //relocate("com.flowpowered.nbt", "com.grinderwolf.swm.internal.nbt")
        relocate("com.zaxxer.hikari", "com.infernalsuite.aswm.internal.hikari")
        relocate("com.mongodb", "com.infernalsuite.aswm.internal.mongodb")
        relocate("io.lettuce", "com.infernalsuite.aswm.internal.lettuce")
        relocate("org.bson", "com.infernalsuite.aswm.internal.bson")
    }

    assemble {
        dependsOn(shadowJar)
    }
}

description = "slimeworldplugin"
