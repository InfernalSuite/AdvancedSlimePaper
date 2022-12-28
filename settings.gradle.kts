pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "slimeworldmanager"

include("plugin")
include("slimeworldmanager-api", "slimeworldmanager-server")
