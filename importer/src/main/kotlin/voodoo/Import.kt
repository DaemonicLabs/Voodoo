package voodoo

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import voodoo.importer.CurseImporter
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object Import : KLogging() {
    suspend fun main(vararg args: String) = runBlocking {
        //        logger.info { args.map { it } }
        logger.debug { args.joinToString(" ") }
        val parser = ArgParser(args)
        val arguments = Arguments(parser)
        parser.force()

        arguments.run {
            logger.info { this.methode }
            val importer = when (methode) {
                "curse" -> CurseImporter

                else -> {
                    logger.error("no such import methode: '$methode'")
                    exitProcess(-1)
                }
            }

            // TODO: import as ModPack and NestedPack ?

            importer.import(source = source, target = target, name = name)

            println("import successful")
        }
    }

    private class Arguments(parser: ArgParser) {
        val methode by parser.positional(
            "METHODE",
            help = "format to import from"
        ) { this.toLowerCase() }
            .default("")

        val source by parser.positional(
            "SOURCE",
            help = "input url/file"
        )

        val target by parser.positional(
            "OUTPUT",
            help = "output file/rootFolder"
        ) { File(this) }
            .default(File("."))

        val name by parser.positional(
            "NAME",
            help = "pack name/id"
        )
            .default<String?>(null)
    }
}