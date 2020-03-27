package voodoo

import com.eyeem.watchadoin.Stopwatch
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

import kotlinx.coroutines.runBlocking
import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.tester.MultiMCTester
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object TesterForDSL : KLogging() {
    fun main(stopwatch: Stopwatch, modpack: LockPack, vararg args: String) = stopwatch {
        runBlocking {
            val arguments = Arguments(ArgParser(args))

            arguments.run {

                //            logger.info("loading $modpackLockFile")
                // TODO: load proper rootDir
//            val modpack = LockPack.parse(modpackLockFile.absoluteFile, File("."))

                val tester = when (methode) {
                    "mmc" -> MultiMCTester

                    else -> {
                        logger.error("no such packing methode: $methode")
                        exitProcess(-1)
                    }
                }

                tester.execute(stopwatch = "$methode-test".watch, modpack = modpack, clean = clean)
            }
        }
    }

    private class Arguments(parser: ArgParser) {
        val methode by parser.positional(
            "METHOD",
            help = "testing provider to use"
        ) { this.toLowerCase() }
            .default("")

        val clean by parser.flagging(
            "--clean", "-c",
            help = "clean output rootFolder before packaging"
        )
            .default(true)
    }
}