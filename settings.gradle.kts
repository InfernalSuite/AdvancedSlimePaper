pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "ASPaper"

include(":gradle:platform")
include(":api")
include(":core")
//include(":importer")
include(":loaders")
include(":plugin")
include(":impl")
include(":impl:aspaper-api")
include(":impl:aspaper-server")
//include(":aspaper-api")
//include(":aspaper-server")

include("loaders:mongo-loader")
findProject(":loaders:mongo-loader")?.name = "mongo-loader"
include("loaders:api-loader")
findProject(":loaders:api-loader")?.name = "api-loader"
include("loaders:file-loader")
findProject(":loaders:file-loader")?.name = "file-loader"
include("loaders:mysql-loader")
findProject(":loaders:mysql-loader")?.name = "mysql-loader"
include("loaders:redis-loader")
findProject(":loaders:redis-loader")?.name = "redis-loader"
