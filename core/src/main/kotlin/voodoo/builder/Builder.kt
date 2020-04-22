package voodoo.builder

import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
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

    fun build(
        stopwatch: Stopwatch,
        modpack: ModPack,
        id: String,
        targetFileName: String = "$id.lock.pack.json",
        targetFile: File = modpack.sourceFolder.resolve(targetFileName),
        noUpdate: Boolean = false,
        entriesFilter: List<String> = listOf()
    ): LockPack = runBlocking {
        stopwatch {
            modpack.entrySet.forEach { entry ->
                logger.info("id: ${entry.id} entry: $entry")
            }

            try {
                resolve(
                    "resolve".watch,
                    modpack,
                    noUpdate = noUpdate && entriesFilter.isEmpty(),
                    updateEntries = entriesFilter
                )
            } catch (e: Exception) {
                e.printStackTrace()
                coroutineContext.cancel()
                exitProcess(1)
            }


            modpack.lockEntrySet.forEach { lockEntry ->
                val provider = Providers[lockEntry.provider]
                if (!provider.validate(lockEntry)) {
                    logger.error { lockEntry }
                    throw IllegalStateException("entry did not validate")
                }
            }

            logger.info("Creating locked pack...")
            val lockedPack = "lock".watch {
                modpack.lock()
            }


            "writeLockEntries".watch {
                lockedPack.writeLockEntries()
            }

            logger.info("Writing lock file... $targetFile")
            targetFile.parentFile.mkdirs()
            targetFile.writeText(lockedPack.toJson(LockPack.serializer()))

            lockedPack
        }
    }
}
