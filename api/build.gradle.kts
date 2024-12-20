plugins {
    id("asp.base-conventions")
    id("asp.publishing-conventions")
}

dependencies {
    api(libs.annotations)
    api(libs.adventure.nbt)

    compileOnly(paperApi())
}

publishConfiguration {
    name = "Advanced Slime Paper API"
    description = "API for Advanced Slime Paper"
}
