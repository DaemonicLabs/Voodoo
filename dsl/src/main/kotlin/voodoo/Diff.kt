package voodoo

import com.eyeem.watchadoin.Stopwatch
import mu.KLogging
import voodoo.changelog.ChangelogBuilder
import voodoo.changelog.PackDiff
import voodoo.data.lock.LockPack
import voodoo.util.*
import java.io.File

object Diff : KLogging() {

    // TODO: differentiate creating a diff for current version (and zip)
    //  and between 2 old versions (unzip)

    private val directories = Directories.get(moduleName = "diff")

    fun writeMetaInfo(
        stopwatch: Stopwatch,
        rootDir: File,
        newPack: LockPack
    ) = stopwatch {
        //TODO: delete or redo from scratch
//        val metaDataLocation = getMetaDataLocation(rootDir, newPack.id)
//        metaDataLocation.deleteRecursively()
//        val newPackMetaInfo = PackDiff.writePackMetaInformation(metaDataLocation, newPack)
//        val newEntryMetaInfo = PackDiff.writeEntryMetaInformation(metaDataLocation, newPack)
//
//        // TODO: find git root directory instead of gradle root
//        val gitRoot = SharedFolders.GitRoot.get()
//        val tagfile = gitRoot.resolve("build").resolve("git-tags").resolve(newPack.id)
//        tagfile.parentFile.mkdirs()
//        // TODO: write git-tag command ?
//        tagfile.writeText(newPack.version)
    }

    private data class GitArchiveFolders(
        val meta: File,
        val source: File
    )

    fun createChangelog(
        stopwatch: Stopwatch,
        docDir: File,
        rootDir: File,
        currentPack: LockPack,
        changelogBuilder: ChangelogBuilder
    ) = stopwatch {
        val cacheHome = directories.cacheHome.resolve("CHANGELOG").resolve(currentPack.id)
        cacheHome.mkdirs()

//        addVersion(rootDir, newPack.id, newPack.version)
        val tags = readVersionTags(currentPack.id)
        logger.debug("tags: $tags")
        // get last version before the current one
//        val lastVersion = versions.lastOrNull().takeIf { it != newPack.version } ?: versions.getOrNull(versions.size-2)
//        logger.debug("lastVersion: $lastVersion")
        if (currentPack.version != tags.lastOrNull() && currentPack.version in tags) {
            throw IllegalArgumentException("version ${currentPack.version} already exists and is not the last version, please do not try to break things")
        }

        val metaFolder = cacheHome.resolve("meta").apply { mkdirs() }
        val sourcesFolder = cacheHome.resolve("sources").apply { mkdirs() }

        // copying current version
        val currentVersion = "${currentPack.version}-dev"
        val currentData = GitArchiveFolders(
            cacheHome.resolve("meta").resolve(currentVersion),
            cacheHome.resolve("sources").resolve(currentVersion)
        )
        run {
            currentData.meta.let { meta ->
                meta.deleteRecursively()
                meta.mkdirs()
                rootDir.resolve(".meta").resolve(currentPack.id).copyRecursively(meta)
            }
            currentData.source.let { source ->
                source.deleteRecursively()
                source.mkdirs()
                rootDir.resolve(currentPack.id).copyRecursively(source)
            }
        }

        val gitArchiveFolder = cacheHome.resolve("git-archive").apply { mkdirs() }

        val versionData = tags.mapNotNull { tag ->
            val version = tag.substringAfter(currentPack.id + "_")
            val metaZip = gitArchiveFolder.resolve("${version}-meta.zip")
            val metaProcessResult = ShellUtil.runProcess(
                "git", "archive",
                "-o", metaZip.path,
                "$tag:.meta/${currentPack.id}/",
                wd = SharedFolders.GitRoot.get()
            )
            val metaExtractFolder =  cacheHome.resolve("meta").resolve(version).apply { mkdirs() }
            metaExtractFolder.deleteRecursively()
            UnzipUtility.unzip(metaZip, metaExtractFolder)

            metaExtractFolder.run {
                // TODO: evaluate if filtering is necessary
                if(!resolve("entry.meta.json").exists() ||
                    !resolve("pack.meta.json").exists()) {
                    return@mapNotNull null
                }
            }

            val sourceZip = gitArchiveFolder.resolve("${version}-source.zip")
            val sourceProcessResult = ShellUtil.runProcess(
                "git", "archive",
                "-o", sourceZip.path,
                "$tag:${currentPack.id}/",
                wd = SharedFolders.GitRoot.get()
            )
            val sourceFolder = cacheHome.resolve("sources").resolve(version).apply { mkdirs() }
            sourceFolder.deleteRecursively()
            UnzipUtility.unzip(sourceZip, sourceFolder)

            version to GitArchiveFolders(metaExtractFolder, sourceFolder)
        }.toMap() + (currentVersion to currentData)

        val versions = tags.map {
            it.substringAfter("${currentPack.id}_")
        }.filter {
            it in versionData
        }

        logger.debug("versions: $versionData")


        val diffPairs = (listOf(null) + versions).zipWithNext()
        val diffFiles = mutableListOf<File>()
        val changelogs = diffPairs.map { (oldVersion, newVersion) ->
            logger.info("generating diff $oldVersion -> $newVersion")
            // load old version
            val oldSource = versionData[oldVersion]?.source
            logger.debug("old root dir: $oldSource")

            val oldLockPackFile = oldSource?.resolve("${currentPack.id}.lock.pack.json")

            val oldPack = try {
                logger.info("reading: $oldLockPackFile")
                if (oldLockPackFile != null) {
                    LockPack.parse(oldLockPackFile, oldSource).also {
                        logger.info("oldPack: ${it.version}")
                    }
                } else null
            } catch (e: Exception) {
                logger.error("could not parse old pack")
                e.printStackTrace()
                null
            }

            val newSource = versionData.getValue(newVersion!!).source
            val newLockPackFile = newSource.resolve("${currentPack.id}.lock.pack.json")

            val newPack = try {
                logger.info("reading: $newLockPackFile")
                LockPack.parse(newLockPackFile, newSource).also {
                    logger.info("newPack: ${it.version}")
                }
            } catch (e: Exception) {
                logger.error("could not parse old pack")
                e.printStackTrace()
                null
            }


            // TODO: create a diff object
            //   diff pack values
            //   diff entries
            //   diff files
//            val diff = PackDiff(
//                newPack = newPack!!,
//                oldPack = oldPack
//            )

            logger.debug("docDir: $docDir")
            val changelog = PackDiff.writeChangelog(
                oldMeta = oldVersion?.let { versionData.getValue(it).meta },
                newMeta = versionData.getValue(newVersion).meta,
                docDir = docDir,
                tmpChangelogFile = cacheHome.resolve("${newVersion}_${changelogBuilder.filename}").apply { parentFile.mkdirs() },
                generator = changelogBuilder
            )
            val diffFile = if (oldSource != null) {
                writeDiff(
                    rootFolder = sourcesFolder,
                    oldSource = oldSource,
                    newSource = newSource,
                    diffFile = cacheHome.resolve("${newVersion}_changes.diff"),
                    docDir = docDir
                )
            } else {
                val emptySource = sourcesFolder.resolve(".empty")
                emptySource.deleteRecursively()
                emptySource.mkdirs()
                writeDiff(
                    rootFolder = sourcesFolder,
                    oldSource = emptySource,
                    newSource = newSource,
                    diffFile = cacheHome.resolve("${newVersion}_changes.diff"),
                    docDir = docDir
                ).also {
                    emptySource.deleteRecursively() // delete the useless empty folder afterwards
                }
            }
            if(diffFile != null) {
                diffFiles += diffFile
            }
            // TODO: find better place to copy to
            // diffFile?.copyTo(newMetaDataLocation.resolve(newPack.version).resolve(diffFile.name), overwrite = true)

            changelog
        }
        val metaSteps = diffPairs.map { (oldVersion, newVersion) ->
            val oldMeta = versionData[oldVersion]?.meta
            val newMeta =  versionData.getValue(newVersion!!).meta
            oldMeta to newMeta
        }
        PackDiff.writeFullChangelog(
            steps = metaSteps,
            docDir = docDir,
            tmpChangelogFile = cacheHome.resolve(currentPack.id).resolve(changelogBuilder.fullFilename).apply { parentFile.mkdirs() },
            generator = changelogBuilder
        )
        docDir.resolve("complete_changes.diff").writeText(
            diffFiles.joinToString("\n\n") { it.readText() }
        )
    }

    fun writeDiff(rootFolder: File, oldSource: File, newSource: File, diffFile: File, docDir: File): File? {
        if (ShellUtil.isInPath("diff")) {
            oldSource.walkBottomUp().forEach {
                when {
                    it.name.endsWith(".lock.json") -> it.delete()
                    it.name.endsWith(".lock.pack.json") -> it.delete()
                }
            }
            newSource.walkBottomUp().forEach {
                when {
                    it.name.endsWith(".lock.json") -> it.delete()
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
