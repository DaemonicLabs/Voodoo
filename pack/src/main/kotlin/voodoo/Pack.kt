package voodoo

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.pack.*
import voodoo.util.json
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object Pack : KLogging() {
    private val packMap = mapOf(
        "sk" to SKPack,
        "mmc" to MMCPack,
        "mmc-static" to MMCStaticPack,
        "mmc-fat" to MMCFatPack,
        "server" to ServerPack,
        "curse" to CursePack
    )

    @JvmStatic
    fun main(vararg args: String) = mainBody {
        val arguments = Arguments(ArgParser(args))

        runBlocking {
            arguments.run {
                logger.info("loading $modpackLockFile")
                val modpack: LockPack = json.parse(modpackLockFile.readText())
                val rootFolder = modpackLockFile.absoluteFile.parentFile
                modpack.loadEntries(rootFolder)

                val packer = packMap[methode.toLowerCase()] ?: run {
                    logger.error("no such packing methode: $methode")
                    exitProcess(-1)
                }

                packer.download(
                    modpack = modpack,
                    folder = File(System.getProperty("user.dir")),
                    target = targetFolder,
                    clean = true
                )
            }
        }
    }

    fun pack(packFile: File, rootFolder: File, vararg args: String) {
        val modpack: LockPack = json.parse(packFile.readText())
        pack(modpack, rootFolder, *args)
    }

    fun pack(modpack: LockPack, rootFolder: File, vararg args: String) {
        val arguments = ArgumentsForDSL(ArgParser(args))

        runBlocking {
            arguments.run {
                modpack.loadEntries(rootFolder)

                val packer = packMap[methode.toLowerCase()] ?: run {
                    logger.error("no such packing methode: $methode")
                    exitProcess(-1)
                }

                packer.download(
                    modpack = modpack,
                    folder = rootFolder,
                    target = target,
                    clean = true
                )
            }
        }
    }

    private class Arguments(parser: ArgParser) {
        val methode by parser.positional(
            "METHODE",
            help = "format to package into"
        ) { this.toLowerCase() }
            .default("")

        val modpackLockFile by parser.positional(
            "FILE",
            help = "input pack .lock.hjson"
        ) { File(this) }

        val targetFolder by parser.storing(
            "--output", "-o",
            help = "output rootFolder"
        ).default<String?>(null)
    }

    private class ArgumentsForDSL(parser: ArgParser) {
        val methode by parser.positional(
            "METHODE",
            help = "format to package into"
        ) { this.toLowerCase() }
            .default("")
        val target by parser.storing(
            "--output", "-o",
            help = "output rootFolder"
        ).default<String?>(null)
    }
}