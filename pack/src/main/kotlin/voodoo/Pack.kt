package voodoo

import com.eyeem.watchadoin.Stopwatch
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.pack.*
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object Pack : KLogging() {
    private val packMap = listOf(
        VoodooPackager,
        MMCSelfupdatingPackVoodoo,
        SKPack,
        MMCSelfupdatingPackSk,
        MMCSelfupdatingFatPackSk,
        MMCFatPack,
        ServerPack,
        CursePack
    ).associateBy { it.id }

//    suspend fun main(vararg args: String) {
//        val arguments = Arguments(ArgParser(args))
//
//        arguments.run {
//            logger.info("loading $modpackLockFile")
//            val modpack: LockPack = json.parse(LockPack.serializer(), modpackLockFile.readText())
//            val rootFolder = modpackLockFile.absoluteFile.parentFile
//            modpack.loadEntries(rootFolder)
//
//            val packer = packMap[methode.toLowerCase()] ?: run {
//                logger.error("no such packing methode: $methode")
//                exitProcess(-1)
//            }
//
//            packer.pack(
//                modpack = modpack,
//                target = targetFolder,
//                clean = true
//            )
//            logger.info("finished packaging")
//        }
//    }

    suspend fun pack(stopwatch: Stopwatch, modpack: LockPack, uploadBaseDir: File, vararg args: String) = stopwatch {
        logger.info("parsing arguments")
        val arguments = Arguments(ArgParser(args))

        arguments.run {
            logger.info("loading entries")
            modpack.loadEntries()

            val packer = packMap[methode.toLowerCase()] ?: run {
                logger.error("no such packing methode: $methode")
                exitProcess(-1)
            }

            val output = with(packer) { uploadBaseDir.getOutputFolder(modpack.id) }
            output.mkdirs()

            packer.pack(
                stopwatch = "${packer.label}-timer".watch,
                modpack = modpack,
                output = output,
                uploadBaseDir = uploadBaseDir,
                clean = true
            )
            logger.info("finished packaging")
        }
    }

    private class Arguments(parser: ArgParser) {
        val methode by parser.positional(
            "METHOD",
            help = "format to package into"
        ) { this.toLowerCase() }
            .default("")
    }
}