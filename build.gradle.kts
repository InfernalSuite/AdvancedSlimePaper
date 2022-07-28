plugins {
    `java-library`
    id("org.kordamp.gradle.profiles") version "0.47.0"
}

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
        mavenCentral()
    }
    dependencies {
        classpath("io.freefair.gradle:lombok-plugin:6.3.0")
    }
}

allprojects {

    group = "com.grinderwolf"
    version = "2.10.0-SNAPSHOT"

    apply(plugin = "java")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "org.kordamp.gradle.profiles")

    repositories {
        mavenLocal()
        mavenCentral()

        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/nms/")
        maven("https://repo.rapture.pw/repository/maven-releases/")
        maven("https://repo.glaremasters.me/repository/concuncan/")
    }

    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }

    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
        (options as StandardJavadocDocletOptions).tags("apiNote:a:API Note:", "implSec:a:Implementation Requirements:", "implNote:a:Implementation Note:")
    }

    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}