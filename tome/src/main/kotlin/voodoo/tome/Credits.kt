package voodoo.tome

import com.github.mustachejava.DefaultMustacheFactory
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.util.readJson
import java.io.File
import java.io.StringWriter


/**
 * Created by nikky on 15/04/18.
 * @author Nikky
 * @version 1.0
 */

object Credits : KLogging() {
    @JvmStatic
    fun main(vararg args: String) = mainBody {
        val arguments = Arguments(ArgParser(args))

        arguments.run {
            val inFile = File(inputArg)
            var modpack = inFile.readJson<LockPack>()

            if (sort) {
                modpack = modpack.copy(entries = modpack.entries.sortedBy { it.name })
            }

            val mf = DefaultMustacheFactory()
            val mustache = mf.compile(templateFile.reader(), templateFile.path)
            val sw = StringWriter()
            mustache.execute(sw, modpack)

            if (stdout) {
                print(sw.toString())
            } else {
                val target = targetArg ?: "${modpack.name}.credits.md"
                val targetFile = File(target)
                logger.info("Writing credits file... $targetFile")
                targetFile.writeText(sw.toString())
            }
        }

    }

    private class Arguments(parser: ArgParser) {
        val inputArg by parser.positional("FILE",
                help = "input pack lock.json")

        val templateFile by parser.positional("TEMPLATE",
                help = "template header") { File(this) }

        val sort by parser.flagging("--sort",
                help = "sort entries alphanumerically")

        val targetArg by parser.storing("--output", "-o",
                help = "output file json")
                .default<String?>(null)

        val stdout by parser.flagging("--stdout", "-s",
                help = "print output")
                .default(false)
    }
}