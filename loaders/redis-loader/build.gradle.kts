plugins {
    id("asp.base-conventions")
    id("asp.publishing-conventions")
}

dependencies {
    compileOnly(project(":api"))

    compileOnlyApi(libs.lettuce)

    compileOnly(paperApi())
}

publishConfiguration {
    name = "Advanced Slime Paper Redis Loader"
    description = "Redis loader for Advanced Slime Paper"
}
