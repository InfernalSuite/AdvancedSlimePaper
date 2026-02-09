plugins {
    id("asp.base-conventions")
    id("asp.publishing-conventions")
}

dependencies {
    compileOnly(project(":api"))

    compileOnlyApi(libs.hikari)
    compileOnly(paperApi())
}

publishConfiguration {
    name = "Advanced Slime Paper SQL Loader"
    description = "SQL loader for Advanced Slime Paper"
}
