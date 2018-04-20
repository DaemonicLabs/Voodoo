package voodoo

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import mu.KLogging
import voodoo.builder.resolve
import voodoo.data.flat.ModPack
import voodoo.util.json
import voodoo.util.readYaml
import voodoo.util.writeJson
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 * @version 1.0
 */

object Builder : KLogging() {
    @JvmStatic
    fun main(vararg args: String) = mainBody {

        val arguments = Arguments(ArgParser(args))

        arguments.run {
            val inFile = File(importArg)
            val modpack = inFile.readYaml<ModPack>()

            modpack.resolve(force, entries)
            if (!nosave) {
                println("saving changes...")
                inFile.writeJson(modpack)
            }

            logger.info("Creating locked pack...")
            val lockedPack = modpack.lock()

            if (stdout) {
                print(lockedPack.json)
            } else {
                var target = targetArg ?: "${lockedPack.name}.lock.json"
                if (!target.endsWith(".json")) target += ".json"
                val targetFile = File(target)
                logger.info("Writing lock file... $targetFile")
                targetFile.writeJson(lockedPack)
            }
        }
    }

    private class Arguments(parser: ArgParser) {
        val importArg by parser.positional("FILE",
                help = "input pack json")

        val targetArg by parser.storing("--output", "-o",
                help = "output file json")
                .default<String?>(null)

        val nosave by parser.flagging("--nosave",
                help = "do not save inputfile after resolve")
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
}

