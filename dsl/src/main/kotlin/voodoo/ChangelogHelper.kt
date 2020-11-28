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
        lockpacks: List<LockPack>,
        docDir: File,
        id: String,
        changelogBuilder: ChangelogBuilder,
    ) = stopwatch {
        val cacheHome = directories.cacheHome.resolve("CHANGELOG").resolve(id)
        cacheHome.mkdirs()

        val diffWorkingDir = cacheHome.resolve("sources").apply { mkdirs() }

        val lockpacks = lockpacks
            .sortedWith(LockPack.versionComparator)

        val (firstChangelog, firstDiffText) = lockpacks.first().let { newPack ->
            createChangelogAndDiff(
                oldPack = null,
                newPack = newPack,
                docDir = docDir,
                changelogBuilder = changelogBuilder,
                diffWorkingDir = diffWorkingDir
            )
        }
        val diffs = lockpacks.zipWithNext().map { (oldPack, newPack) ->
            val (changelogFile, diffText) = createChangelogAndDiff(
                oldPack = oldPack,
                newPack = newPack,
                docDir = docDir,
                changelogBuilder = changelogBuilder,
                diffWorkingDir = diffWorkingDir
            )
            diffText
        }

        val addDiffTexts = listOfNotNull(
            firstDiffText,
            *diffs.toTypedArray()
        )

        docDir.resolve("complete_changes.diff").writeText(
            addDiffTexts.joinToString("\n\n")
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
        changelogBuilder: ChangelogBuilder,
        diffWorkingDir: File,
    ): Pair<File, String?> {
        logger.info("generating diff ${oldPack?.version} -> ${newPack.version}")

        val newSourceCopy = diffWorkingDir.resolve(newPack.version).apply {
            deleteRecursively()
            mkdirs()
            newPack.sourceFolder.takeIf { it.exists() }
                ?.copyRecursively(this, overwrite = true)
        }

        val oldSourceCopy = diffWorkingDir.resolve(oldPack?.version ?: ".empty").apply {
            deleteRecursively()
            mkdirs()
            oldPack?.sourceFolder?.takeIf { it.exists() }
                ?.copyRecursively(this, overwrite = true)
        }

        logger.debug("docDir: $docDir")

        val diffText = executeDiff(
            rootFolder = diffWorkingDir,
            oldSource = oldSourceCopy,
            newSource = newSourceCopy,
        )
        if (diffText != null) {
            val targetFile = docDir.resolve(newPack.version).resolve("changes.diff")
            targetFile.parentFile.mkdirs()
            targetFile.writeText(diffText)
        }

        val currentChangelogText = buildString {
            with(receiver = changelogBuilder) {
                writeChangelog(
                    oldPack = oldPack,
                    newPack = newPack
                )
            }
        }
        val changelogFile = docDir.resolve(newPack.version).resolve(changelogBuilder.filename).apply {
            logger.info("writing changelog to ${this}")
            absoluteFile.parentFile.mkdirs()
            writeText(currentChangelogText)
        }

        newSourceCopy.deleteRecursively()
        oldSourceCopy.deleteRecursively()

        return changelogFile to diffText
    }

    private fun executeDiff(
        rootFolder: File,
        oldSource: File,
        newSource: File,
    ): String? {
        if (ShellUtil.isInPath("git")) {
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
            return diffResult.stdout.trim().takeIf { it.isNotBlank() }
        } else {
            logger.error("please install `git`")
            return null
        }
    }
}
