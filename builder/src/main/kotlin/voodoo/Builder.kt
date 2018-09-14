package voodoo

import com.xenomachina.argparser.*
import kotlinx.coroutines.experimental.cancel
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.serialization.json.JSON
import mu.KLogging
import voodoo.builder.resolve
import voodoo.data.flat.ModPack
import voodoo.provider.Provider
import voodoo.util.ExceptionHelper
import voodoo.util.json
import java.io.File
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object Builder : KLogging() {
    @JvmStatic
    fun main(vararg args: String) = mainBody {
        val parser = ArgParser(args)
        val arguments = Arguments(parser)
        parser.force()

        runBlocking(context = ExceptionHelper.context) {
            arguments.run {
                val modpack: ModPack = JSON.unquoted.parse(packFile.readText())

                val parentFolder = packFile.absoluteFile.parentFile

                modpack.loadEntries(parentFolder)

                modpack.entrySet.forEach { entry ->
                    logger.info("id: ${entry.id} entry: $entry")
                }

                try {
                    modpack.resolve(
                        this@runBlocking,
                        parentFolder,
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
                    val provider = Provider.valueOf(lockEntry.provider).base
                    if (!provider.validate(lockEntry)) {
                        logger.error { lockEntry }
                        throw IllegalStateException("entry did not validate")
                    }
                }

                logger.info("Creating locked pack...")
                val lockedPack = modpack.lock()
                lockedPack.rootFolder = parentFolder
                lockedPack.entrySet.clear()
                lockedPack.entrySet += modpack.lockEntrySet

                lockedPack.writeLockEntries()

                if (stdout) {
                    print(lockedPack.json)
                } else {
                    val file = targetFile ?: parentFolder.resolve("${lockedPack.id}.lock.json")
                    logger.info("Writing lock file... $file")
                    file.writeText(JSON.unquoted.stringify(lockedPack))
                }

                //TODO: generate modlist

                logger.info("writing modlist")
                val sw = StringWriter()
                sw.append(lockedPack.report)
                sw.append("\n")

                modpack.lockEntrySet.sortedBy { it.name().toLowerCase() }.forEach { entry ->
                    val provider = Provider.valueOf(entry.provider).base
                    sw.append("\n\n")
                    sw.append(provider.report(entry))
                }

                val modlist = (targetFile ?: File(".")).absoluteFile.parentFile.resolve("modlist.md")
                modlist.writeText(sw.toString())
            }
        }
    }
}

private class Arguments(parser: ArgParser) {
    val packFile by parser.positional(
        "FILE",
        help = "input pack json"
    ) { File(this) }

    val targetFile by parser.storing(
        "--output", "-o",
        help = "output file json"
    ) { File(this) }
        .default<File?>(null)

    val stdout by parser.flagging(
        "--stdout", "-s",
        help = "print output"
    )
        .default(false)

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
