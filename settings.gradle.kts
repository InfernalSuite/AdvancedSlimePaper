pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }

}


rootProject.name = "slimeworldmanager"
include(":slimeworldmanager-api")
include(":slimeworldmanager-classmodifier")
include(":slimeworldmanager-classmodifierapi")
include(":slimeworldmanager-plugin")
include(":slimeworldmanager-importer")
include(":slimeworldmanager-nms-common")
include(":slimeworldmanager-nms-v118-2")
include("slimeworldmanager-nms-v119")
include("slimeworldmanager-nms-v119-1")
