plugins {
    id("asp.paperweight-conventions")
    id("com.gradleup.shadow") version "8.3.5" apply false
    java
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()

        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/nms/")
        maven("https://repo.rapture.pw/repository/maven-releases/")
        maven("https://repo.glaremasters.me/repository/concuncan/")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}
