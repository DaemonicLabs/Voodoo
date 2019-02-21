package voodoo

import mu.KLogging
import voodoo.changelog.ChangelogBuilder
import voodoo.changelog.PackDiff
import voodoo.data.lock.LockPack
import voodoo.util.ArchiveUtil
import voodoo.util.Directories
import voodoo.util.ShellUtil
import voodoo.util.UnzipUtility
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
        val (tag, archiveZip) = ArchiveUtil.archiveLast(rootDir) ?: run {
            logger.error("archive failed")
            return null
        }
        require(archiveZip.exists()) { "git archive did not create a file" }

        val oldRootDir = directories.cacheHome.resolve(rootDir.name) // TODO: use hash of path + id instead ?
        oldRootDir.deleteRecursively()
        oldRootDir.mkdirs()

        UnzipUtility.unzip(archiveZip, oldRootDir)

        // assume id and paths stayed the same

        val oldLockPackFile = oldRootDir.resolve(newPack.sourceFolder.relativeTo(newPack.rootDir))
            .resolve("${newPack.id}.lock.pack.hjson")
        if (!oldLockPackFile.exists()) {
            logger.error("$oldLockPackFile does not exist")
            return null
        }

        val oldPack = try {
            logger.info("reading: $oldLockPackFile")
            LockPack.parse(oldLockPackFile, oldRootDir).also {
                logger.info("oldPack: ${it.version}")
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
        val diff = PackDiff(
            newPack = newPack,
            oldPack = oldPack,
            oldEntries = oldPack?.entrySet?.associateBy { it.id },
            newEntries = newPack.entrySet.associateBy { it.id },
            newRootDir = rootDir,
            oldRootDir = oldRootDir
        )

        val oldMetaDataLocation = readMetaDataLocation(oldRootDir, oldPack?.id ?: newPack.id)
        val newMetaDataLocation = getMetaDataDefault(rootDir, newPack.id)
        val metaDataPointerFile = getMetaDataPointer(rootDir, newPack.id)
        metaDataPointerFile.writeText(newMetaDataLocation.relativeTo(rootDir).unixPath)

        writeGitDiff(
            newMeta = newMetaDataLocation,
            docDir = docDir,
            tag = tag,
            subDir = oldPack?.sourceFolder?.relativeTo(oldRootDir) ?: newPack.sourceFolder.relativeTo(rootDir)
        )

        diff.writeChangelog(
            newMeta = newMetaDataLocation,
            oldMeta = oldMetaDataLocation,
            docDir = docDir,
            generator = changelogBuilder
        )
        return diff
    }

    fun writeGitDiff(newMeta: File, docDir: File, tag: String, subDir: File): File? {
        if (ShellUtil.isInPath("git")) {
            newMeta.mkdirs()
            val diffFile = newMeta.resolve("changes.diff")
            val diffResult = ShellUtil.runProcess(
                "git",
                "diff",
                tag,
                "--",
                subDir.path,
                ":(exclude)*.lock.hjson",
                ":(exclude)*.lock.pack.hjson"
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
