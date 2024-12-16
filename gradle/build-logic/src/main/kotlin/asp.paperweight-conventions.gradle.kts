plugins {
    id("io.papermc.paperweight.patcher")
}

repositories {
    mavenCentral()
    maven(PAPER_MAVEN_PUBLIC_URL) {
        content { onlyForConfigurations(configurations.paperclip.name) }
    }
}

dependencies {
    remapper("net.fabricmc:tiny-remapper:0.10.3:fat")
    decompiler("org.vineflower:vineflower:1.10.1")
    paperclip("io.papermc:paperclip:3.0.3")
}

paperweight {
    serverProject.set(project(":aspaper-server"))

    remapRepo.set(PAPER_MAVEN_PUBLIC_URL)
    decompileRepo.set(PAPER_MAVEN_PUBLIC_URL)

    usePaperUpstream(providers.gradleProperty("paperRef")) {
        withPaperPatcher {
            apiPatchDir.set(layout.projectDirectory.dir("patches/api"))
            apiOutputDir.set(layout.projectDirectory.dir("aspaper-api"))

            serverPatchDir.set(layout.projectDirectory.dir("patches/server"))
            serverOutputDir.set(layout.projectDirectory.dir("aspaper-server"))

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
