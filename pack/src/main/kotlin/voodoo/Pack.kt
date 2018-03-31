package voodoo

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import mu.KotlinLogging
import voodoo.data.lock.LockPack
import voodoo.pack.SKPack
import voodoo.util.readJson
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 * @version 1.0
 */

private val logger = KotlinLogging.logger {}

fun main(vararg args: String) = mainBody {

    val arguments = Arguments(ArgParser(args))

    arguments.run {
        val inFile = File(importArg)
        val modpack = inFile.readJson<LockPack>()

        val methode = methodeArg.toLowerCase()

        val packer = when(methode) {
            "sk" -> SKPack

            else -> {
                logger.error("no such packing methode: $methode")
                exitProcess(-1)
            }
        }

        packer.download(modpack = modpack, target = targetArg, clean = true)
    }
}

private class Arguments(parser: ArgParser) {
    val importArg by parser.positional("FILE",
            help = "input pack json")

    val methodeArg by parser.positional("METHODE",
            help = "input pack json")
            .default("")

    val targetArg by parser.storing("--output", "-o",
            help = "output folder")
            .default<String?>(null)
}