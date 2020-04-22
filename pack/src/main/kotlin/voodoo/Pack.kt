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
    val packMap = listOf(
        VoodooPackager,
        MMCSelfupdatingPackVoodoo,
        SKPack,
        MMCSelfupdatingPackSk,
        MMCSelfupdatingFatPackSk,
        MMCFatPack,
        ServerPack,
        CursePack
    ).associateBy { it.id }

    suspend fun pack(stopwatch: Stopwatch, modpack: LockPack, uploadBaseDir: File, packer: AbstractPack) = stopwatch {
        logger.info("parsing arguments")

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

    private class Arguments(parser: ArgParser) {
        val methode by parser.positional(
            "METHOD",
            help = "format to package into"
        ) { this.toLowerCase() }
            .default("")
    }
}