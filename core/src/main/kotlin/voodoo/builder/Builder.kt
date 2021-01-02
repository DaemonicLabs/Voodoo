package voodoo.builder

import com.eyeem.watchadoin.Stopwatch
import mu.KLogging
import voodoo.data.flat.FlatModPack
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.provider.Providers
import voodoo.util.packToZip
import voodoo.util.toJson

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object Builder : KLogging() {
    suspend fun lock(
        stopwatch: Stopwatch,
        modpack: FlatModPack
    ): LockPack = stopwatch {
        val targetFile = LockPack.fileForVersion(baseDir = modpack.baseFolder, version = modpack.version)
        val targetFolder = LockPack.baseFolderForVersion(baseDir = modpack.baseFolder, version = modpack.version)

        modpack.entrySet.forEach { entry ->
            logger.info("id: ${entry.id} entry: $entry")
        }

        logger.info("Creating locked pack...")
        val lockedPack = modpack.lock("lock".watch, targetFolder)
        lockedPack.lockBaseFolder.deleteRecursively()
        lockedPack.lockBaseFolder.mkdirs()

        logger.info("Writing lock file... $targetFile")
        targetFile.parentFile.mkdirs()
        targetFile.writeText(lockedPack.toJson(LockPack.serializer()))

        logger.info { "copying input files into output" }
        logger.info { "copying from ${modpack.sourceFolder}" }
        lockedPack.sourceFolder.also { sourceFolder ->
            sourceFolder.deleteRecursively()
            sourceFolder.mkdirs()
            modpack.sourceFolder.copyRecursively(sourceFolder, overwrite = true)

            if(sourceFolder.list()?.isNotEmpty() == true) {
                packToZip(
                    sourceDir = sourceFolder,
                    zipFile = lockedPack.sourceZip,
                    preserveTimestamps = false
                )
            }
            sourceFolder.deleteRecursively()
        }
        logger.info { "copying: ${modpack.iconFile}" }
        modpack.iconFile
            .takeIf { it.exists() }
            ?.copyTo(lockedPack.iconFile, overwrite = true)

        logger.info { "copying from ${modpack.localFolder}" }
        lockedPack.localFolder.also { localFolder ->
            localFolder.deleteRecursively()
            localFolder.mkdirs()
            lockedPack.entries.filterIsInstance<LockEntry.Local>()
                .forEach { entry ->
                    val localTargetFile = localFolder.resolve(entry.fileSrc)
                    logger.info { "copying: $localTargetFile" }
                    localTargetFile.absoluteFile.parentFile.mkdirs()
                    modpack.localFolder.resolve(entry.fileSrc).copyTo(localTargetFile, overwrite = true)
                }

            if(localFolder.list()?.isNotEmpty() == true) {
                packToZip(
                    sourceDir = localFolder,
                    zipFile = lockedPack.localZip,
                    preserveTimestamps = false
                )
            }
            localFolder.deleteRecursively()
        }

        lockedPack
    }
}
