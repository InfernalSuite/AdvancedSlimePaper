plugins {
    id("asp.base-conventions")
    id("asp.publishing-conventions")
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(paperApi())

    api(libs.mongo)
}

publishConfiguration {
    name = "Advanced Slime Paper MongoDB Loader"
    description = "MongoDB GridFS Loader for Advanced Slime Paper"
}
