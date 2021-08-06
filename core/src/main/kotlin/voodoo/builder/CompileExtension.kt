package voodoo.builder

import com.eyeem.watchadoin.Stopwatch
import mu.KotlinLogging
import voodoo.data.flat.FlatModPack
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.util.packToZip
import voodoo.util.toJson

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

private val logger = KotlinLogging.logger { }
suspend fun FlatModPack.compile(
    stopwatch: Stopwatch,
    noModUpdates: Boolean = false
): LockPack = stopwatch {
    val modpack = this@compile
    val targetFile = modpack.baseFolder.resolve(LockPack.FILENAME)
    val targetFolder = modpack.baseFolder

    modpack.entrySet.forEach { entry ->
        logger.info("id: ${entry.id} entry: $entry")
    }

    val prevLockpack = try {
        if (targetFile.exists()) {
            LockPack.parse(targetFile, baseFolder)
        } else {
            null
        }
    } catch (e: Exception) {
        logger.error(e) { "failed to parse old pack" }
        null
    }

    logger.info("Creating locked pack...")
    val lockedPack = modpack.lock("lock".watch, targetFolder, noModUpdates, prevLockpack)
    logger.info { "locked: $lockedPack" }
//    lockedPack.lockBaseFolder.deleteRecursively()
//    lockedPack.lockBaseFolder.mkdirs()

    logger.info("Writing lock file... $targetFile")
//    targetFile.parentFile.mkdirs()
    targetFile.writeText(lockedPack.toJson(LockPack.versionedSerializer))

    logger.info { "copying input files into output" }
    logger.info { "copying from ${modpack.sourceFolder}" }
//    lockedPack.sourceFolder.also { sourceFolder ->
//        sourceFolder.deleteRecursively()
//        sourceFolder.mkdirs()
//        modpack.sourceFolder.copyRecursively(sourceFolder, overwrite = true)
//        modpack.sourceFolder.walkTopDown().filter { it.isFile }.forEach { file ->
//            val relative = file.relativeTo(modpack.sourceFolder)
//            sourceFolder.resolve(relative).setLastModified(file.lastModified())
//        }
//
//        if (sourceFolder.list()?.isNotEmpty() == true) {
//            packToZip(
//                sourceDir = modpack.sourceFolder,
//                zipFile = lockedPack.sourceZip,
////                    preserveTimestamps = false
//            )
//        }
//        sourceFolder.deleteRecursively()
//    }
//    logger.info { "copying: ${modpack.iconFile}" }
//    modpack.iconFile
//        .takeIf { it.exists() }
//        ?.copyTo(lockedPack.iconFile, overwrite = true)
//
//    logger.info { "copying from ${modpack.localFolder}" }
//    lockedPack.localFolder.also { localFolder ->
//        localFolder.deleteRecursively()
//        localFolder.mkdirs()
//        lockedPack.entries.filterIsInstance<LockEntry.Local>()
//            .forEach { entry ->
//                val localTargetFile = localFolder.resolve(entry.fileSrc)
//                logger.info { "copying: $localTargetFile" }
//                localTargetFile.absoluteFile.parentFile.mkdirs()
//                modpack.localFolder.resolve(entry.fileSrc).copyTo(localTargetFile, overwrite = true)
//                localTargetFile.setLastModified(modpack.localFolder.resolve(entry.fileSrc).lastModified())
//            }
//
//        if (localFolder.list()?.isNotEmpty() == true) {
//            packToZip(
//                sourceDir = localFolder,
//                zipFile = lockedPack.localZip,
////                    preserveTimestamps = false
//            )
//        }
//        localFolder.deleteRecursively()
//    }

    lockedPack
}
