plugins {
    java
    `maven-publish`
    id("io.github.goooler.shadow") version "8.1.5" apply false
    id("io.papermc.paperweight.patcher") version "1.6.3"
    id("org.kordamp.gradle.profiles") version "0.47.0"
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

repositories {
    mavenCentral()
    maven(paperMavenPublicUrl) {
        content { onlyForConfigurations(configurations.paperclip.name) }
    }
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()

        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/nms/")
        maven("https://repo.rapture.pw/repository/maven-releases/")
        maven("https://repo.glaremasters.me/repository/concuncan/")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencies {
    remapper("net.fabricmc:tiny-remapper:0.10.1:fat")
    decompiler("org.vineflower:vineflower:1.10.1")
    paperclip("io.papermc:paperclip:3.0.3")
}

subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }
    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
    }
}

paperweight {
    serverProject.set(project(":slimeworldmanager-server"))

    remapRepo.set(paperMavenPublicUrl)
    decompileRepo.set(paperMavenPublicUrl)

    usePaperUpstream(providers.gradleProperty("paperRef")) {
        withPaperPatcher {
            apiPatchDir.set(layout.projectDirectory.dir("patches/api"))
            apiOutputDir.set(layout.projectDirectory.dir("slimeworldmanager-api"))

            serverPatchDir.set(layout.projectDirectory.dir("patches/server"))
            serverOutputDir.set(layout.projectDirectory.dir("slimeworldmanager-server"))

            patchTasks {
                register("generatedApi") {
                    isBareDirectory.set(true)
                    upstreamDirPath.set("paper-api-generator/generated")
                    patchDir.set(layout.projectDirectory.dir("patches/generatedApi"))
                    outputDir.set(layout.projectDirectory.dir("paper-api-generator/generated"))
                }
            }
        }
    }
}
