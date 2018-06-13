package voodoo

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.tester.MultiMCTester
import voodoo.util.readJson
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object Tester : KLogging() {
    @JvmStatic
    fun main(vararg args: String) = mainBody {

        val arguments = Arguments(ArgParser(args))

        arguments.run {
            val tester = when (methode) {
                "mmc" -> MultiMCTester

                else -> {
                    logger.error("no such packing methode: $methode")
                    exitProcess(-1)
                }
            }

            tester.execute(modpack = modpack, clean = clean)
        }
    }

    private class Arguments(parser: ArgParser) {
        val methode by parser.positional("METHODE",
                help = "testing provider to use") { this.toLowerCase()}
                .default("")

        val modpack by parser.positional("FILE",
                help = "input pack .lock.json") { File(this).readJson<LockPack>() }

        val clean by parser.flagging("--clean", "-c",
                help = "clean output folder before packaging")
                .default(true)
    }
}