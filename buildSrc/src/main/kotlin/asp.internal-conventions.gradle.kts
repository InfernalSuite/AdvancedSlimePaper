import net.kyori.indra.git.IndraGitExtension
import org.gradle.kotlin.dsl.the

version = "${rootProject.providers.gradleProperty("apiVersion").get()}-${the<IndraGitExtension>().commit()?.name ?: "SNAPSHOT"}"
