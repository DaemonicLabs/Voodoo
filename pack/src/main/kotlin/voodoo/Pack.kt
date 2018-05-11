package voodoo

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.pack.SKPack
import voodoo.pack.ServerPack
import voodoo.pack.ServerPackSparse
import voodoo.util.readJson
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 * @version 1.0
 */

object Pack : KLogging() {
    @JvmStatic
    fun main(vararg args: String) = mainBody {

        val arguments = Arguments(ArgParser(args))

        arguments.run {
            val inFile = File(importArg)
            val modpack = inFile.readJson<LockPack>()

            val methode = methodeArg.toLowerCase()

            val packer = when (methode) {
                "sk" -> SKPack
                "serverFull" -> ServerPack
                "server" -> ServerPackSparse

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
                help = "input pack lock.json")

        val methodeArg by parser.positional("METHODE",
                help = "format to package into")
                .default("")

        val targetArg by parser.storing("--output", "-o",
                help = "output folder")
                .default<String?>(null)
    }
}