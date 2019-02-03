package voodoo.util

import mu.KLogging
import java.io.File

object ArchiveUtil : KLogging() {
    private val directories = Directories.get(moduleName = "git-archive")

    fun archiveLast(directory: File = File("."), identifier: String = directory.name): Pair<String, File>? {
        ShellUtil.requireInPath("git", "requires git")

        // TODO: use a kotlin wrapper over JGit instead
        val tags = ShellUtil.runProcess("git", "tag", wd = directory.absoluteFile, displayOut = false)
            .stdout.trim().lines()

        if (tags.isEmpty()) run {
            logger.error("no tags to archive found")
            return null
        }
        tags.forEach {
            logger.info("tag: $it")
        }
        val tag = tags.last()
        val zipFile = archive(directory = directory, tag = tag, identifier = identifier) ?: return null
        return tag to zipFile
//        UnzipUtility.unzip(zipFile = zipFile, destDirectory = )
        // TODO: unzip archive
    }

    fun archive(directory: File = File("."), tag: String, identifier: String = directory.name): File? {
        val cacheHome = directories.cacheHome
        val zipFile = cacheHome.resolve("$identifier.zip")
        // TODO: use a kotlin wrapper over JGit instead
        ShellUtil.runProcess(
            "git",
            "archive",
            "--format",
            "zip",
            "--output",
            zipFile.absolutePath,
            tag,
            wd = directory.absoluteFile
        )
        return zipFile.takeIf { it.exists() }
    }
}
