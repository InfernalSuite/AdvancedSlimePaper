import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.the
import java.nio.file.Path

val Project.libs: LibrariesForLibs
    get() = the()

// Utils for working with java.nio.file.Path from a FileSystemLocation
// Courtesy of PaperMC (io.papermc.paperweight.util/file.kt) <3
val FileSystemLocation.path: Path
    get() = asFile.toPath()

val Provider<out FileSystemLocation>.path: Path
    get() = get().path

val Provider<out FileSystemLocation>.pathOrNull: Path?
    get() = orNull?.path

fun Project.paperApi(): Dependency =
    dependencies.create("io.papermc.paper:paper-api:${rootProject.providers.gradleProperty("version").get()}")
