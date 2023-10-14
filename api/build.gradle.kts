plugins {
    `java-library`
     `maven-publish`
    signing
}

dependencies {
    api("com.flowpowered:flow-nbt:2.0.2")
    api("org.jetbrains:annotations:23.0.0")

    compileOnly("io.papermc.paper:paper-api:1.20.2-R0.1-SNAPSHOT")
}

java {
    withSourcesJar()
    withJavadocJar()
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
                            name.set("Advanced Slime World Manager API")
                            description.set("API for ASP")
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
                                url.set("https://github.com/Paul19988/Advanced-Slime-World-Manager/")
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
