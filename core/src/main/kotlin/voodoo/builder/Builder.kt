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

suspend fun FlatModPack.compile(
    stopwatch: Stopwatch
): LockPack = stopwatch {
    val modpack = this@compile
    val targetFile = LockPack.fileForVersion(baseDir = modpack.baseFolder, version = modpack.version)
    val targetFolder = LockPack.baseFolderForVersion(baseDir = modpack.baseFolder, version = modpack.version)

    modpack.entrySet.forEach { entry ->
        Builder.logger.info("id: ${entry.id} entry: $entry")
    }

    Builder.logger.info("Creating locked pack...")
    val lockedPack = modpack.lock("lock".watch, targetFolder)
    lockedPack.lockBaseFolder.deleteRecursively()
    lockedPack.lockBaseFolder.mkdirs()

    Builder.logger.info("Writing lock file... $targetFile")
    targetFile.parentFile.mkdirs()
    targetFile.writeText(lockedPack.toJson(LockPack.serializer()))

    Builder.logger.info { "copying input files into output" }
    Builder.logger.info { "copying from ${modpack.sourceFolder}" }
    lockedPack.sourceFolder.also { sourceFolder ->
        sourceFolder.deleteRecursively()
        sourceFolder.mkdirs()
        modpack.sourceFolder.copyRecursively(sourceFolder, overwrite = true)
        modpack.sourceFolder.walkTopDown().filter { it.isFile }.forEach { file ->
            val relative = file.relativeTo(modpack.sourceFolder)
            sourceFolder.resolve(relative).setLastModified(file.lastModified())
        }

        if(sourceFolder.list()?.isNotEmpty() == true) {
            packToZip(
                sourceDir = modpack.sourceFolder,
                zipFile = lockedPack.sourceZip,
//                    preserveTimestamps = false
            )
        }
        sourceFolder.deleteRecursively()
    }
    Builder.logger.info { "copying: ${modpack.iconFile}" }
    modpack.iconFile
        .takeIf { it.exists() }
        ?.copyTo(lockedPack.iconFile, overwrite = true)

    Builder.logger.info { "copying from ${modpack.localFolder}" }
    lockedPack.localFolder.also { localFolder ->
        localFolder.deleteRecursively()
        localFolder.mkdirs()
        lockedPack.entries.filterIsInstance<LockEntry.Local>()
            .forEach { entry ->
                val localTargetFile = localFolder.resolve(entry.fileSrc)
                Builder.logger.info { "copying: $localTargetFile" }
                localTargetFile.absoluteFile.parentFile.mkdirs()
                modpack.localFolder.resolve(entry.fileSrc).copyTo(localTargetFile, overwrite = true)
                localTargetFile.setLastModified(modpack.localFolder.resolve(entry.fileSrc).lastModified())
            }

        if(localFolder.list()?.isNotEmpty() == true) {
            packToZip(
                sourceDir = localFolder,
                zipFile = lockedPack.localZip,
//                    preserveTimestamps = false
            )
        }
        localFolder.deleteRecursively()
    }

    lockedPack
}

object Builder : KLogging() {
    @Deprecated("use compile", ReplaceWith("modpack.compile(stopwatch)"))
    suspend fun lock(
        stopwatch: Stopwatch,
        modpack: FlatModPack
    ): LockPack = modpack.compile(stopwatch)
}