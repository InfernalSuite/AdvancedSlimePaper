plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("xyz.jpenilla.run-paper") version "1.0.6"
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":core"))

    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.mongodb:mongo-java-driver:3.12.11")
    implementation("io.lettuce:lettuce-core:6.2.0.RELEASE")
    implementation("org.spongepowered:configurate-yaml:4.1.2")
    implementation("org.bstats:bstats-bukkit:3.0.0")
    implementation("commons-io:commons-io:2.11.0")
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
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

    runServer {
        minecraftVersion("1.19.2")
    }
}



description = "slimeworldmanager-plugin"
