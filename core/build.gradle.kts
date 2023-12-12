plugins {
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":slimeworldmanager-api"))
    implementation("com.github.luben:zstd-jni:1.5.2-2")
}
