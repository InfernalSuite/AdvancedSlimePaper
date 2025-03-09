import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.5" apply false
    id("io.papermc.paperweight.patcher") version "2.0.0-beta.14"
    id("org.kordamp.gradle.profiles") version "0.54.0"
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

paperweight {
    upstreams.paper {
        ref = providers.gradleProperty("paperCommit")

        patchFile {
            path = "paper-server/build.gradle.kts"
            outputFile = file("slimeworldmanager-server/build.gradle.kts")
            patchFile = file("slimeworldmanager-server/build.gradle.kts.patch")
        }
        patchFile {
            path = "paper-api/build.gradle.kts"
            outputFile = file("slimeworldmanager-api/build.gradle.kts")
            patchFile = file("slimeworldmanager-api/build.gradle.kts.patch")
        }
        patchDir("paperApi") {
            upstreamPath = "paper-api"
            excludes = setOf("build.gradle.kts")
            patchesDir = file("slimeworldmanager-api/paper-patches")
            outputDir = file("paper-api")
        }
    }
}

//paperweight {
//    serverProject.set(project(":slimeworldmanager-server"))
//
//    remapRepo.set(paperMavenPublicUrl)
//    decompileRepo.set(paperMavenPublicUrl)
//
//    usePaperUpstream(providers.gradleProperty("paperRef")) {
//        withPaperPatcher {
//            apiPatchDir.set(layout.projectDirectory.dir("patches/api"))
//            apiOutputDir.set(layout.projectDirectory.dir("slimeworldmanager-api"))
//
//            serverPatchDir.set(layout.projectDirectory.dir("patches/server"))
//            serverOutputDir.set(layout.projectDirectory.dir("slimeworldmanager-server"))
//
//            patchTasks {
//                register("generatedApi") {
//                    isBareDirectory.set(true)
//                    upstreamDirPath.set("paper-api-generator/generated")
//                    patchDir.set(layout.projectDirectory.dir("patches/generatedApi"))
//                    outputDir.set(layout.projectDirectory.dir("paper-api-generator/generated"))
//                }
//            }
//        }
//    }
//}

repositories {
    mavenCentral()
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
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
        options.isFork = true
    }
    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }
    tasks.withType<Test> {
        testLogging {
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events(TestLogEvent.STANDARD_OUT)
        }
    }
    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
        maven("https://jitpack.io")
    }
}
