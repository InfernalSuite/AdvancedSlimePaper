plugins {
    `kotlin-dsl`
}

group = "com.infernalsuite"

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

fun convertPlugin(plugin: Provider<PluginDependency>): String {
    val id = plugin.get().pluginId
    return "$id:$id.gradle.plugin:${plugin.get().version}"
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(convertPlugin(libs.plugins.blossom))
    implementation(convertPlugin(libs.plugins.indragit))
    implementation(convertPlugin(libs.plugins.profiles))
    implementation(convertPlugin(libs.plugins.kotlin.jvm))
    implementation(convertPlugin(libs.plugins.lombok))
    implementation(convertPlugin(libs.plugins.paperweight.patcher))
    implementation(convertPlugin(libs.plugins.plugin.yml.paper))
    implementation(convertPlugin(libs.plugins.shadow))
}
