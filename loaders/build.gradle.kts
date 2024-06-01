plugins {
    id("java")
    `java-library`
    `maven-publish`
    signing
}

group = "com.infernalsuite.aswm"
version = "3.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":api"))

    api("com.zaxxer:HikariCP:5.1.0")
    api("org.mongodb:mongodb-driver-sync:5.1.0")
    api("io.lettuce:lettuce-core:6.3.2.RELEASE")

    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("commons-io:commons-io:2.11.0")
}

profiles {
    profile("publish") {
        activation {
            property {
                setKey("publish")
                setValue("true")
            }
        }
        action {
            publishing {
                publications {
                    create<MavenPublication>("maven") {
                        groupId = "${project.group}"
                        artifactId = project.name
                        version = "${project.version}"

                        from(components["java"])

                        pom {
                            name.set("Advanced Slime Paper Reference Loaders")
                            description.set("Reference loader implementations for ASP")
                            url.set("https://github.com/InfernalSuite/AdvancedSlimePaper")
                            licenses {
                                license {
                                    name.set("GNU General Public License, Version 3.0")
                                    url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                                }
                            }
                            developers {
                                developer {
                                    id.set("InfernalSuite")
                                    name.set("The InfernalSuite Team")
                                    url.set("https://github.com/InfernalSuite")
                                    email.set("infernalsuite@gmail.com")
                                }
                            }
                            scm {
                                connection.set("scm:git:https://github.com:InfernalSuite/AdvancedSlimePaper.git")
                                developerConnection.set("scm:git:ssh://github.com:InfernalSuite/AdvancedSlimePaper.git")
                                url.set("https://github.com/InfernalSuite/AdvancedSlimePaper/")
                            }
                            issueManagement {
                                system.set("Github")
                                url.set("https://github.com/InfernalSuite/AdvancedSlimePaper/issues")
                            }
                        }

                        versionMapping {
                            usage("java-api") {
                                fromResolutionOf("runtimeClasspath")
                            }
                            usage("java-runtime") {
                                fromResolutionResult()
                            }
                        }
                    }
                }
                repositories {
                    maven {
                        name = "infernalsuite"
                        url = uri("https://repo.infernalsuite.com/repository/maven-snapshots/")
                        credentials {
                            username = project.property("ISUsername") as String?
                            password = project.property("ISPassword") as String?
                        }
                    }
                }
            }

            signing {
                useGpgCmd()
                sign(publishing.publications["maven"])
            }
        }
    }
}