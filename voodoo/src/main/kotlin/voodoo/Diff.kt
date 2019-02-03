package voodoo

import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.util.ArchiveUtil
import voodoo.util.Directories
import voodoo.util.UnzipUtility
import java.io.File

object Diff : KLogging() {

    val directories = Directories.get(moduleName = "diff")
    fun createDiff(rootDir: File, newPack: LockPack): PackDiff? {
        val (tag, archiveZip) = ArchiveUtil.archiveLast(rootDir) ?: run {
            logger.error("archive failed")
            return null
        }
        require(archiveZip.exists()) { "git archive did not create a file" }

        val oldpackFolder = directories.cacheHome.resolve(rootDir.name) // TODO: use hash of path + id instead ?
        oldpackFolder.deleteRecursively()
        oldpackFolder.mkdirs()

        UnzipUtility.unzip(archiveZip, oldpackFolder)

        // assume id and paths stayed the same

        val oldLockPackFile = oldpackFolder.resolve(newPack.sourceFolder.relativeTo(newPack.rootDir))
            .resolve("${newPack.id}.lock.pack.hjson")
        if (!oldLockPackFile.exists()) {
            logger.error("$oldLockPackFile does not exist")
            return null
        }


        return try {
            logger.info("reading: $oldLockPackFile")
            val oldPack = LockPack.parse(oldLockPackFile, oldpackFolder)

            logger.info("oldPack: ${oldPack.version}")

            // TODO: create a diff object
            //   diff pack values
            //   diff entries
            //   diff files
            PackDiff(
                oldPack,
                newPack,
                oldEntries = oldPack.entrySet.associateBy { it.id },
                newEntries = newPack.entrySet.associateBy { it.id },
                oldSource = oldPack.sourceFolder,
                newSource = newPack.sourceFolder,
                oldTag = tag
            )
        } catch (e: Exception) {
            logger.error("could not parse old pack")
            e.printStackTrace()
            null
        }
    }
}
