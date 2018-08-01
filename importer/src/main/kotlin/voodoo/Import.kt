package voodoo

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import voodoo.importer.CurseImporter
import voodoo.importer.YamlImporter
import java.io.File
import java.util.concurrent.CompletableFuture.runAsync
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object Import : KLogging() {
    @JvmStatic
    fun main(vararg args: String) = mainBody {
        //        logger.info { args.map { it } }
        logger.debug { args.joinToString(" ") }
        val parser = ArgParser(args)
        val arguments = Arguments(parser)
        parser.force()

        arguments.run {
            logger.info { this.methode }
            val tester = when (methode) {
                "curse" -> CurseImporter
                "yaml" -> YamlImporter

                else -> {
                    logger.error("no such import methode: '$methode'")
                    exitProcess(-1)
                }
            }

            //TODO: import as ModPack and NestedPack ?

            runBlocking { tester.import(source = source, target = target) }

            println("import successful")
        }
    }

    private class Arguments(parser: ArgParser) {
        val methode by parser.positional("METHODE",
                help = "format to import from") { this.toLowerCase() }
                .default("")

        val source by parser.positional("SOURCE",
                help = "input url/file")

        val target by parser.positional("OUTPUT",
                help = "output file/folder") { File(this) }
    }
}