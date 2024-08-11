plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("net.kyori.blossom") version "2.1.0"
}

version = "3.0.0"

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
    implementation("org.spongepowered:configurate-yaml:4.1.2")
    implementation("org.bstats:bstats-bukkit:3.0.0")
    implementation("org.incendo:cloud-paper:2.0.0-beta.9")
    implementation("org.incendo:cloud-minecraft-extras:2.0.0-beta.9")
    implementation("org.incendo:cloud-annotations:2.0.0-rc.1")
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveClassifier.set("")

        relocate("org.bstats", "com.grinderwolf.swm.internal.bstats")
        relocate("ninja.leaping.configurate", "com.grinderwolf.swm.internal.configurate")
        //relocate("com.flowpowered.nbt", "com.grinderwolf.swm.internal.nbt")
        relocate("com.zaxxer.hikari", "com.grinderwolf.swm.internal.hikari")
        relocate("com.mongodb", "com.grinderwolf.swm.internal.mongodb")
        relocate("io.lettuce", "com.grinderwolf.swm.internal.lettuce")
        relocate("org.bson", "com.grinderwolf.swm.internal.bson")
    }

    assemble {
        dependsOn(shadowJar)
    }
}

description = "slimeworldplugin"
