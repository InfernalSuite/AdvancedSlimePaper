plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":slimeworldmanager-api"))
    implementation("com.github.luben:zstd-jni:1.5.7-1")
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
}
