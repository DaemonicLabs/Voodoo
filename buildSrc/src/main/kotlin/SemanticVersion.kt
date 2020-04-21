import org.gradle.api.Project
import java.io.File
import java.util.*

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemanticVersion> {
    fun incrementMajor() = copy(
        major = major + 1,
        minor = 0,
        patch = 0
    )

    fun incrementMinor() = copy(
        minor = minor + 1,
        patch = 0
    )

    fun incrementPatch() = copy(
        patch = patch + 1
    )

    fun write(project: Project, filename: String = ".meta/version.properties") {
        val versionFile = project.projectDir.resolve(filename)
        val properties = Properties()
        properties.setProperty("major", major.toString())
        properties.setProperty("minor", minor.toString())
        properties.setProperty("patch", patch.toString())
        versionFile.parentFile.mkdirs()
        versionFile.bufferedWriter().use {
            properties.store(it, null)
        }
    }

    companion object {
        val filename = ".meta/version.properties"
        val lastFilename = "build/previous.version.properties"
        fun parse(file: File): SemanticVersion {
            val properties = Properties()
            file.bufferedReader().use {
                properties.load(it)
            }
            return SemanticVersion(
                properties.getProperty("major").toInt(),
                properties.getProperty("minor").toInt(),
                properties.getProperty("patch").toInt()
            )
        }

        fun read(project: Project, filename: String = SemanticVersion.filename): SemanticVersion {
            val versionFile = project.projectDir.resolve(filename)
            return parse(versionFile)
        }

        fun readLast(project: Project, filename: String = lastFilename): SemanticVersion {
            val versionFile = project.projectDir.resolve(filename)
            if (!versionFile.exists()) {
                val newerVersionFile = project.projectDir.resolve("version.properties")
                newerVersionFile.copyTo(versionFile)
            }
            return parse(versionFile)
        }
        fun readLastIfExists(project: Project, filename: String = lastFilename): SemanticVersion? {
            val versionFile = project.projectDir.resolve(filename)
            if (!versionFile.exists()) {
                return null
            }
            return parse(versionFile)
        }
    }

    override fun compareTo(other: SemanticVersion): Int {
        return major.compareTo(other.major).takeUnless { it == 0 }
            ?: minor.compareTo(other.minor).takeUnless { it == 0 }
            ?: patch.compareTo(other.patch)
    }

    override fun toString(): String = "${major}.${minor}.${patch}"
}