package voodoo

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.provider.Provider
import voodoo.util.readJson
import java.io.File


/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 * @version 1.0
 */

object Tome : KLogging() {
    @JvmStatic
    fun main(vararg args: String) = mainBody {
        val arguments = Arguments(ArgParser(args))

        arguments.run {
            val inFile = File(inputArg)
            val modpack = inFile.readJson<LockPack>()
            val builder = StringBuilder()
            if (headerFile != null) {
                val header = headerFile!!.readText()
                        .replace("[packName]", modpack.name)
                        .replace("[packTitle]", modpack.title)
                        .replace("[packVersion]", modpack.version)
                builder.append(header)
                builder.append("\n")
            }

            val template = templateFile.readText()

            val entries = if (sort) {
                modpack.entries.sortedBy { it.name }
            } else {
                modpack.entries
            }

            for (entry in entries) {
                logger.info("processing ${entry.name}")
                val provider = Provider.valueOf(entry.provider).base

                val section = template
                        .replace("[modName]", entry.name)
                        .replace("[authors]", provider.getAuthors(entry, modpack).joinToString(", "))
                        .replace("[projectPage]", provider.getProjectPage(entry, modpack))

                builder.append(section)
                builder.append("\n")
            }

            if (footerFile != null) {
                val footer = footerFile!!.readText()
                        .replace("[packName]", modpack.name)
                        .replace("[packTitle]", modpack.title)
                        .replace("[packVersion]", modpack.version)
                builder.append(footer)
            }

            if (stdout) {
                print(builder.toString())
            } else {
                var target = targetArg ?: "${modpack.name}.md"
                if (!target.endsWith(".md")) target += ".md"
                val targetFile = File(target)
                logger.info("Writing modlist file... $targetFile")
                targetFile.writeText(builder.toString())
            }
        }

    }

    private class Arguments(parser: ArgParser) {
        val inputArg by parser.positional("FILE",
                help = "input pack lock.json")

        val templateFile by parser.positional("TEMPLATE",
                help = "template header") { File(this) }

        val headerFile by parser.storing("--header",
                help = "template header") { File(this) }
                .default<File?>(null)

        val footerFile by parser.storing("--footer",
                help = "template header") { File(this) }
                .default<File?>(null)

        val sort by parser.flagging("--sort",
                help = "template header")

        val targetArg by parser.storing("--output", "-o",
                help = "output file json")
                .default<String?>(null)

        val stdout by parser.flagging("--stdout", "-s",
                help = "print output")
                .default(false)
    }
}