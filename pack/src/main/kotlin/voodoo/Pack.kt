package voodoo

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.pack.*
import voodoo.util.readJson
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object Pack : KLogging() {
    @JvmStatic
    fun main(vararg args: String) = mainBody {

        val arguments = Arguments(ArgParser(args))

        arguments.run {
            val packer = when (methode) {
                "sk" -> SKPack
                "mmc" -> MMCPack
                "mmc-static" -> MMCStaticPack
                "server" -> ServerPack
                "curse" -> CursePack

                else -> {
                    logger.error("no such packing methode: $methode")
                    exitProcess(-1)
                }
            }

            packer.download(modpack = modpack, target = targetArg, clean = true)
        }
    }

    private class Arguments(parser: ArgParser) {
        val methode by parser.positional("METHODE",
                help = "format to package into") { this.toLowerCase()}
                .default("")

        val modpack by parser.positional("FILE",
                help = "input pack .lock.json") { File(this).readJson<LockPack>() }

        val targetArg by parser.storing("--output", "-o",
                help = "output folder")
                .default<String?>(null)

//        val clean by parser.flagging("--clean", "-c",
//                help = "clean output folder before packaging")
//                .default(true)
    }
}