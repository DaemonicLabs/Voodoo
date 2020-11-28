package voodoo

import com.eyeem.watchadoin.Stopwatch
import mu.KLogging
import voodoo.changelog.ChangelogBuilder
import voodoo.data.lock.LockPack
import voodoo.util.*
import java.io.File

object ChangelogHelper : KLogging() {
    private val directories = Directories.get(moduleName = "diff")

    fun createAllChangelogs(
        stopwatch: Stopwatch,
        docDir: File,
        id: String,
        baseDir: File,
        changelogBuilder: ChangelogBuilder
    ) = stopwatch {
        val cacheHome = directories.cacheHome.resolve("CHANGELOG").resolve(id)
        cacheHome.mkdirs()

        val diffWorkingDir = cacheHome.resolve("sources").apply { mkdirs() }

        val lockpacks = LockPack.parseAll(baseFolder = baseDir)
            .sortedWith(LockPack.versionComparator)

        val diffFiles = mutableListOf<File>()
        val firstChangelog = createChangelogAndDiff(
            oldPack = null,
            newPack = lockpacks.first(),
    //                versionData = versionData,
            docDir = docDir,
            cacheHome = cacheHome,
            changelogBuilder = changelogBuilder,
            diffWorkingDir = diffWorkingDir,
            diffFiles = diffFiles
        )
        val changelogs = lockpacks.zipWithNext().map { (oldPack, newPack) ->
            createChangelogAndDiff(
                oldPack = oldPack,
                newPack = newPack,
        //                versionData = versionData,
                docDir = docDir,
                cacheHome = cacheHome,
                changelogBuilder = changelogBuilder,
                diffWorkingDir = diffWorkingDir,
                diffFiles = diffFiles
            )
        }

        docDir.resolve("complete_changes.diff").writeText(
            diffFiles.joinToString("\n\n") { it.readText() }
        )

        logger.info { "writing full changelog" }
        with(changelogBuilder) {
            val fullChangelogText = buildString {
                writeFullChangelog(lockpacks.zipWithNext())
            }
            val fullChangelogFile = docDir.resolve(fullFilename)
            fullChangelogFile.writeText(fullChangelogText)
        }
    }

    private fun createChangelogAndDiff(
        newPack: LockPack,
        oldPack: LockPack?,
        docDir: File,
        cacheHome: File,
        changelogBuilder: ChangelogBuilder,
        diffWorkingDir: File,
        diffFiles: MutableList<File>,
    ): File {
        logger.info("generating diff ${oldPack?.version} -> ${newPack?.version}")

        val newSourceCopy = diffWorkingDir.resolve(newPack.version).apply {
            deleteRecursively()
            mkdirs()
            newPack.sourceFolder.takeIf { it.exists() }
                ?.copyRecursively(this, overwrite = true)
        }

        val oldSourceCopy = diffWorkingDir.resolve(newPack.version).apply {
            deleteRecursively()
            mkdirs()
            oldPack?.sourceFolder?.takeIf { it.exists() }
                ?.copyRecursively(this, overwrite = true)
        }

        logger.debug("docDir: $docDir")

        val diffFile = writeDiff(
            rootFolder = diffWorkingDir,
            oldSource = oldSourceCopy,
            newSource = newSourceCopy,
            diffFile = cacheHome.resolve("${newPack.version}_changes.diff"),
            docDir = docDir
        )
        if (diffFile != null) {
            diffFiles += diffFile
        }
        // TODO: find better place to copy to
        // diffFile?.copyTo(newMetaDataLocation.resolve(newPack.version).resolve(diffFile.name), overwrite = true)

        val changelogFile = cacheHome.resolve("${newPack.version}_${changelogBuilder.filename}").apply {
            parentFile.mkdirs()
        }
        val currentChangelogText = buildString {
            with(receiver = changelogBuilder
            ) {
                writeChangelog(
                    oldPack = oldPack,
                    newPack = newPack
                )
            }
        }
        logger.info("writing changelog to ${changelogFile}")
        changelogFile.absoluteFile.parentFile.mkdirs()
        changelogFile.writeText(currentChangelogText)

        docDir.mkdirs()
        changelogFile.copyTo(docDir.resolve(changelogBuilder.filename), overwrite = true)

        newSourceCopy.deleteRecursively()
        oldSourceCopy.deleteRecursively()

        return changelogFile
    }

    fun writeDiff(rootFolder: File, oldSource: File, newSource: File, diffFile: File, docDir: File): File? {
        if (ShellUtil.isInPath("diff")) {
            oldSource.walkBottomUp().forEach {
                when {
                    it.name.endsWith(".lock.pack.json") -> it.delete()
                }
            }
            newSource.walkBottomUp().forEach {
                when {
                    it.name.endsWith(".lock.pack.json") -> it.delete()
                }
            }
//            val diffFile = newSource.resolve("changes.diff")
            // git diff --no-index -- 1.0.3 ':(exclude)*.lock.json' ':(exclude)*.lock.pack.json' 1.0.4 ':(exclude)*.lock.json' ':(exclude)*.lock.pack.json'

            val diffResult = ShellUtil.runProcess(
                "git", "diff",
                "--no-index",
                oldSource.toRelativeString(rootFolder),
                newSource.toRelativeString(rootFolder),
                wd = rootFolder
            )
            logger.info("writing '$diffFile'")
            diffResult.stdout.trim().takeIf { it.isNotBlank() }?.let {
                diffFile.writeText(it)
            } ?: run {
                logger.info("diff result is empty")
                diffFile.delete()
                return null
            }

            diffFile.copyTo(docDir.resolve("changes.diff"), overwrite = true)
            return diffFile
        } else {
            logger.error("please install `diff`")
            return null
        }
    }

    fun getMetaDataLocation(rootDir: File, id: String) = rootDir.resolve(".meta").resolve(id.toLowerCase())

    fun readVersionTags(id: String): List<String> {
        //TODO: get git tags
        val processResult = ShellUtil.runProcess("git", "tag", wd = SharedFolders.GitRoot.get())
        val tags = processResult.stdout.lines().filter {
            it.startsWith(id + "_")
        }
        return tags
    }

//    fun addVersion(rootDir: File, id: String, version: String) {
//        val versionsFile = getMetaDataLocation(rootDir, id).resolve("versions.txt")
//        versionsFile.parentFile.mkdirs()
//        val versions =
//            (versionsFile.takeIf { it.exists() }?.readLines()?.filter { it.isNotBlank() } ?: listOf()).toSet() + version
//        versionsFile.writeText(versions.joinToString("\n"))
//    }
}
