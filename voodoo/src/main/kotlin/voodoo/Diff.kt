package voodoo

import mu.KLogging
import voodoo.changelog.ChangelogBuilder
import voodoo.changelog.PackDiff
import voodoo.data.lock.LockPack
import voodoo.util.Directories
import voodoo.util.ShellUtil
import voodoo.util.unixPath
import java.io.File

object Diff : KLogging() {

    private val directories = Directories.get(moduleName = "diff")
    fun createDiff(
        docDir: File,
        rootDir: File,
        newPack: LockPack,
        changelogBuilder: ChangelogBuilder
    ): PackDiff? {
        val versions = readVersions(rootDir, newPack.id)
        val lastVersion = versions.lastOrNull().takeIf { it != newPack.version }
        if (newPack.version != versions.lastOrNull() && newPack.version in versions) {
            throw IllegalArgumentException("version ${newPack.version} already exists and is not the last version, please do not try to break things")
        }

        val newMetaDataLocation = getMetaDataDefault(rootDir, newPack.id)
        val metaDataPointerFile = getMetaDataPointer(rootDir, newPack.id)

        // copy new pack to .meta/packid/version/root
        val packVersionFolder = newMetaDataLocation.resolve(newPack.version).resolve("pack")
        packVersionFolder.deleteRecursively()
        newPack.sourceFolder.copyRecursively(packVersionFolder)

        // TODO: load old version
        val oldVersionFolder = lastVersion?.let { version -> newMetaDataLocation.resolve(version).resolve("pack") }
        logger.debug("old root dir: $oldVersionFolder")

        val oldLockPackFile = oldVersionFolder
//            ?.resolve(newPack.sourceFolder.relativeTo(newPack.rootDir))
            ?.resolve("${newPack.id}.lock.pack.hjson")
//        if (!oldLockPackFile?.exists()) {
//            logger.error("$oldLockPackFile does not exist")
////            return null
//        }

        val oldPack = try {
            logger.info("reading: $oldLockPackFile")
            if (oldLockPackFile != null && oldVersionFolder != null) {
                LockPack.parse(oldLockPackFile, oldVersionFolder).also {
                    logger.info("oldPack: ${it.version}")
                }
            } else null
        } catch (e: Exception) {
            logger.error("could not parse old pack")
            e.printStackTrace()
            null
        }

        // TODO: create a diff object
        //   diff pack values
        //   diff entries
        //   diff files
        val diff = PackDiff(
            newPack = newPack,
            oldPack = oldPack
        )

        addVersion(rootDir, newPack.id, newPack.version)

        metaDataPointerFile.writeText(newMetaDataLocation.relativeTo(rootDir).unixPath)
        if (oldVersionFolder != null) {
//            val packs = versions.associate { version ->
//                val oldRootDir = newMetaDataLocation.resolve(version).resolve("pack")
//                val oldLockPackFile = oldRootDir
////                    .resolve(newPack.sourceFolder.relativeTo(newPack.rootDir))
//                    .resolve("${newPack.id}.lock.pack.hjson")
//                version to LockPack.parse(oldLockPackFile, oldRootDir).also {
//                    logger.info("oldPack: ${it.version}")
//                }
//            }
//            versions.zipWithNext { lastVersion, currentVersion ->
//                val lastPack = packs[lastVersion]!!
//                val newPack = packs[currentVersion]!!
//                // TODO: write changelogs
//                val diff = PackDiff(
//                    newPack = newPack,
//                    oldPack = lastPack
//                )
//                diff.writeChangelog(
//                    newMeta = newMetaDataLocation.resolve(currentVersion),
//                    oldMeta = oldMetaDataLocation.resolve(lastVersion),
//                    docDir = docDir,
//                    generator = changelogBuilder
//                )
//            }
//            writeDiff(
//                meta = newMetaDataLocation,
//                newFolder = packVersionFolder,
//                oldFolder = oldVersionFolder,
//                docDir = docDir
//            )
        }

        val oldMetaDataLocation = readMetaDataLocation(rootDir, oldPack?.id ?: newPack.id)
        logger.debug("docDir: $docDir")
        diff.writeChangelog(
            newMeta = newMetaDataLocation.resolve(newPack.version),
            oldMeta = lastVersion?.let {oldMetaDataLocation.resolve(it)},
            docDir = docDir,
            generator = changelogBuilder
        )
        diff.writeFullChangelog(newMetaDataLocation,  readVersions(rootDir, newPack.id), docDir = docDir)

        return diff
    }

    fun writeDiff(meta: File, oldFolder: File, newFolder: File, docDir: File): File? {
        if (ShellUtil.isInPath("git")) {
            newFolder.mkdirs()
            val diffFile = meta.resolve("changes.diff")
            val diffResult = ShellUtil.runProcess(
                "git",
                "diff",
                "--",
                oldFolder.toRelativeString(meta),
                newFolder.toRelativeString(meta),
                ":(exclude)*.lock.hjson",
                ":(exclude)*.lock.pack.hjson",
                wd = meta
            )
            logger.info("writing '$diffFile'")
            diffResult.stdout.trim().takeIf { it.isNotBlank() }?.let {
                diffFile.writeText(it)
            } ?: run {
                logger.info("diff result is empty")
                diffFile.delete()
                return null
            }

            return diffFile.copyTo(docDir.resolve(diffFile.name), overwrite = true)
        } else {
            logger.error("please install `git`")
            return null
        }
    }

    fun getMetaDataDefault(rootDir: File, id: String) = rootDir.resolve(".meta").resolve(id.toLowerCase())
    fun getMetaDataPointer(rootDir: File, id: String) = rootDir.resolve(".meta").resolve(id.toLowerCase() + ".txt")

    fun readVersions(rootDir: File, id: String): List<String> {
        val versionsFile = getMetaDataDefault(rootDir, id).resolve("versions.txt")
        return versionsFile.takeIf { it.exists() }?.readLines()?.filter { it.isNotBlank() } ?: listOf()
    }

    fun addVersion(rootDir: File, id: String, version: String) {
        val versionsFile = getMetaDataDefault(rootDir, id).resolve("versions.txt")
        val versions =
            (versionsFile.takeIf { it.exists() }?.readLines()?.filter { it.isNotBlank() } ?: listOf()).toSet() + version
        versionsFile.writeText(versions.joinToString("\n"))
    }

    fun readMetaDataLocation(
        rootDir: File,
        id: String,
        defaultLocation: File = getMetaDataDefault(rootDir, id)
    ): File {
        // TODO: where will the file be stored?
        // rootDir/.meta/$id.txt

        val metaDataPointerFile = getMetaDataPointer(rootDir, id)
        return metaDataPointerFile
            .takeIf { it.exists() }
            ?.run { rootDir.resolve(readText()) }
            ?: run {
                metaDataPointerFile.parentFile.mkdirs()
                metaDataPointerFile.writeText(defaultLocation.relativeTo(rootDir).path.replace('\\', '/'))
                defaultLocation
            }
    }
}
