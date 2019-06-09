package voodoo.builder

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
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
    fun build(
        modpack: ModPack,
        id: String,
        targetFileName: String = "$id.lock.pack.hjson",
        targetFile: File = modpack.sourceFolder.resolve(targetFileName),
        vararg args: String
    ): LockPack = runBlocking {
        logger.debug("parsing args: ${args.joinToString(", ")}")
        val parser = ArgParser(args)
        val arguments = Arguments(parser)
        parser.force()

        arguments.run {
            modpack.entrySet.forEach { entry ->
                logger.info("categoryId: ${entry.id} entry: $entry")
            }

            try {
                resolve(
                    modpack,
                    noUpdate = noUpdate && entries.isEmpty(),
                    updateEntries = entries
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
            val lockedPack = modpack.lock()
            lockedPack.entrySet.clear()
            lockedPack.entrySet += modpack.lockEntrySet

            lockedPack.writeLockEntries()

            logger.info("Writing lock file... $targetFile")
            targetFile.parentFile.mkdirs()
            targetFile.writeText(lockedPack.toJson(LockPack.serializer()))

            lockedPack
        }
    }

    private class Arguments(parser: ArgParser) {
        val noUpdate by parser.flagging(
            "--noUpdate",
            help = "do not update entries"
        )
            .default(false)

        val entries by parser.adding(
            "-E", help = "select specific entries to update"
        )
    }
}
