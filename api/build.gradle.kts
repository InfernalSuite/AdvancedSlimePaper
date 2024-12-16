plugins {
    id("asp.base-conventions")
    id("asp.publishing-conventions")
}

dependencies {
    api(libs.annotations)
    api(libs.adventure.nbt)
    api("com.flowpowered:flow-nbt:2.0.2")

    compileOnly(paperApi())
}

publishConfiguration {
    name = "Advanced Slime Paper API"
    description = "API for Advanced Slime Paper"
}
