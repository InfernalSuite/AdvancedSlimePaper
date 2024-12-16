pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
    includeBuild("gradle/build-logic")
}

rootProject.name = "ASPaper"

include(":gradle:platform")
include(":api")
include(":core")
include(":importer")
include(":loaders")
include(":plugin")
include(":aspaper-api")
include(":aspaper-server")
