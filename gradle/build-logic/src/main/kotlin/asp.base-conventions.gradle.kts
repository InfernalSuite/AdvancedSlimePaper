plugins {
    `java-library`
    id("com.gorylenko.gradle-git-properties")
}

group = rootProject.providers.gradleProperty("group")
version = rootProject.providers.gradleProperty("apiVersion")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(JAVA_VERSION))
    }
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenLocal()
    mavenCentral()

    maven(PAPER_MAVEN_PUBLIC_URL)

    maven("https://repo.codemc.io/repository/nms/")
    maven("https://repo.rapture.pw/repository/maven-releases/")
    maven("https://repo.glaremasters.me/repository/concuncan/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    api(platform(project(":gradle:platform")))
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(JAVA_VERSION)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
        (options as StandardJavadocDocletOptions)
            .tags("apiNote:a:API Note", "implSpec:a:Implementation Requirements", "implNote:a:Implementation Note")
    }

    val git = withType<com.gorylenko.GenerateGitPropertiesTask> {
        outputs.upToDateWhen { false }
        gitProperties.extProperty = "git"
    }.first()

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        dependsOn(git)
        filesMatching(listOf("paper-plugin.yml", "version.txt")) {
            expand("gitCommitId" to git.generatedProperties["git.commit.id"])
        }
    }
}
