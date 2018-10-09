package voodoo

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import kotlinx.coroutines.experimental.cancel
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import voodoo.builder.resolve
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
        name: String,
        targetFileName: String = "$name.lock.hjson",
        targetFile: File = modpack.rootDir.resolve(targetFileName),
        vararg args: String
    ): LockPack = runBlocking {
        val parser = ArgParser(args)
        val arguments = Arguments(parser)
        parser.force()

        arguments.run {
            modpack.entrySet.forEach { entry ->
                logger.info("id: ${entry.id} entry: $entry")
            }

            try {
                resolve(
                    modpack,
                    updateAll = updateAll,
                    updateDependencies = updateDependencies,
                    updateEntries = entries
                )
            } catch (e: Exception) {
                e.printStackTrace()
                coroutineContext.cancel(e)
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
            targetFile.writeText(lockedPack.toJson)

            lockedPack
        }
    }

    private class Arguments(parser: ArgParser) {
        val updateDependencies by parser.flagging(
            "--updateDependencies", "-d",
            help = "update all dependencies"
        )
            .default(false)

        val updateAll by parser.flagging(
            "--updateAll", "-u",
            help = "update all entries, implies updating dependencies"
        )
            .default(false)

        val entries by parser.adding(
            "-E", help = "entries to update"
        )
    }
}
