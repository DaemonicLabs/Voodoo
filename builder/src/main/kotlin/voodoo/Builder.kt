package voodoo

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import mu.KotlinLogging
import voodoo.core.data.flat.ModPack
import voodoo.util.json
import voodoo.util.readYaml
import voodoo.util.writeJson
import java.io.File

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
        val modpack = inFile.readYaml<ModPack>()

        modpack.resolve(force, entries)
        if (save) {
            println("saving changes...")
            inFile.writeJson(modpack)
        }

        logger.info("Creating locked pack...")
        val lockedPack = modpack.lock()

        if (stdout) {
            print(lockedPack.json)
        }
        if (targetArg.isNotEmpty()) {
            logger.info("Writing lock file...")
            var target = targetArg
            if (!target.endsWith(".json")) target += ".json"
            val targetFile = File(target)
            targetFile.writeJson(lockedPack)
        }
    }
}

private class Arguments(parser: ArgParser) {
    val importArg by parser.positional("FILE",
            help = "input pack json")

    val targetArg by parser.storing("--output", "-o",
            help = "output file json")
            .default("")

    val save by parser.flagging("--save",
            help = "save inputfile after resolve")
            .default(true)

    val stdout by parser.flagging("--stdout", "-s",
            help = "print output")
            .default(false)

    val force by parser.flagging("--force", "-f",
            help = "print output")
            .default(false)

    val entries by parser.adding(
            "-E", help = "entries to update")
}