plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api(libs.adventure.nbt)
        api(libs.auto.service)
        api(libs.auto.service.annotations)
        api(libs.bstats)
        api(platform(libs.cloud.bom))
        api(platform(libs.cloud.minecraft.bom))
        api(libs.configurate.core)
        api(libs.configurate.yaml)
        api(libs.hikari)
        api(libs.lettuce)
        api(libs.lombok)
        api(libs.mongo)
        api(libs.slf4j.api)
        api(libs.zstd)
    }
}
