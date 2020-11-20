package voodoo.builder

import com.eyeem.watchadoin.Stopwatch
import mu.KLogging
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockPack
import voodoo.provider.Providers
import voodoo.util.toJson
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object Builder : KLogging() {

    /***
     * @param noUpdate
     * @param entriesFilter only updates the entries with the specified ids
     */

    suspend fun build(
        stopwatch: Stopwatch,
        modpack: ModPack,
        id: String,
        targetFileName: String = "$id.lock.pack.json",
        targetFile: File = modpack.sourceFolder.resolve(targetFileName)
    ): LockPack = stopwatch {
        modpack.entrySet.forEach { entry ->
            logger.info("id: ${entry.id} entry: $entry")
        }

        "resolve". watch {
            try {
                resolve(
                    this,
                    modpack
                )
            } catch (e: Exception) {
                e.printStackTrace()
//                coroutineContext.cancel()
                exitProcess(1)
            }
        }


        "validate".watch {
            modpack.lockEntrySet.forEach { lockEntry ->
                val provider = Providers[lockEntry.provider]
                if (!provider.validate(lockEntry)) {
                    logger.error { lockEntry }
                    throw IllegalStateException("entry did not validate")
                }
            }
        }

        logger.info("Creating locked pack...")
        val lockedPack = modpack.lock("lock".watch)

        "writeLockEntries".watch {
            lockedPack.writeLockEntries()
        }

        logger.info("Writing lock file... $targetFile")
        targetFile.parentFile.mkdirs()
        targetFile.writeText(lockedPack.toJson(LockPack.serializer()))

        lockedPack
    }
}
