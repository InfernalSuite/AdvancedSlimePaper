plugins {
    id("asp.base-conventions")
    id("asp.publishing-conventions")
    id("net.minecrell.plugin-yml.paper")
    id("com.gradleup.shadow")
}

dependencies {
    compileOnly(project(":api"))
    implementation(project(":loaders"))

    implementation(libs.configurate.yaml)
    implementation(libs.bstats)
    implementation(libs.cloud.paper)
    implementation(libs.cloud.minecraft.extras)
    implementation(libs.cloud.annotations)

    compileOnly(paperApi())
}

tasks {
    withType<Jar> {
        archiveBaseName.set("asp-plugin")
    }

    shadowJar {
        archiveClassifier.set("")
        
        relocate("org.bstats", "com.infernalsuite.asp.libs.bstats")
        relocate("org.spongepowered.configurate", "com.infernalsuite.asp.libs.configurate")
        relocate("org.bson", "com.infernalsuite.asp.libs.bson")
    }

    assemble {
        dependsOn(shadowJar)
    }
}

paper {
    name = "ASPaperPlugin"
    description = "ASP plugin for Paper, providing utilities for the ASP platform"
    version = "\${gitCommitId}"
    apiVersion = "1.21"
    main = "com.infernalsuite.asp.plugin.SWPlugin"
    authors = listOf("InfernalSuite")
    bootstrapper = "com.infernalsuite.asp.plugin.SlimePluginBootstrap"
    loader = "com.infernalsuite.asp.plugin.SlimePluginLoader"
}
