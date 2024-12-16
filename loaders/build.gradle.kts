plugins {
    id("asp.base-conventions")
    id("asp.publishing-conventions")
}

dependencies {
    compileOnly(project(":api"))

    api(libs.hikari)
    api(libs.mongo)
    api(libs.lettuce)

    compileOnly(paperApi())
}

publishConfiguration {
    name = "Advanced Slime Paper Loaders"
    description = "Default loaders for Advanced Slime Paper"
}
