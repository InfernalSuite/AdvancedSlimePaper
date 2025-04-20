plugins {
    id("asp.base-conventions")
    id("asp.publishing-conventions")
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(paperApi())
    implementation(libs.zstd)
}

publishConfiguration {
    name = "Advanced Slime Paper Core"
    description = "Core logic for Advanced Slime Paper"
}
