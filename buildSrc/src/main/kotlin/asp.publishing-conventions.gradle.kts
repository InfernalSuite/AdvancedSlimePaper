import com.infernalsuite.asp.conventions.PublishConfiguration.Companion.publishConfiguration
import org.kordamp.gradle.plugin.profiles.ProfilesExtension

plugins {
    `maven-publish`
    signing
    id("org.kordamp.gradle.profiles")
}

val publishConfiguration = publishConfiguration()

extensions.configure<ProfilesExtension>("profiles") {
    profile("publish") {
        activation {
            property {
                setKey("publish")
                setValue("true")
            }
        }
        action {
            extensions.configure<PublishingExtension>("publishing") {
                publications {
                    create<MavenPublication>("maven") {
                        groupId = "${project.group}"
                        artifactId = project.name
                        version = "${project.version}"

                        from(components["java"])

                        pom {
                            name.set(publishConfiguration.name)
                            description.set(publishConfiguration.description)
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
            extensions.configure<SigningExtension>("signing") {
                useGpgCmd()
                sign(extensions.getByName<PublishingExtension>("publishing").publications["maven"])
            }
        }
    }
}