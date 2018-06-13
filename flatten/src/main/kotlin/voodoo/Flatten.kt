package voodoo

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import mu.KLogging
import voodoo.data.nested.NestedPack
import voodoo.util.json
import voodoo.util.readYaml
import voodoo.util.writeJson
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object Flatten: KLogging() {
    @JvmStatic
    fun main(vararg args: String) = mainBody {

        val arguments = Arguments(ArgParser(args))

        arguments.run {
            val inFile = File(importArg)
            println(inFile.absolutePath)
            val nestedPack = inFile.readYaml<NestedPack>()
            var target = targetArg

            logger.info("FLATTENING...")
            val flatpack = nestedPack.flatten(inFile.parentFile)

            if (stdout) {
                print(flatpack.json)
            }
            if (!nofile){
                if (target.isEmpty()) target = inFile.nameWithoutExtension
                if (!target.endsWith(".json")) target += ".json"
                val targetFile = File(target)
                logger.info("writing output to $targetFile")
                targetFile.writeJson(flatpack)
            }
        }
    }

    private class Arguments(parser: ArgParser) {
        val importArg by parser.positional("FILE",
                help = "input pack yaml")

        val targetArg by parser.storing("--output", "-o",
                help = "output file json").default("")

        val stdout by parser.flagging("--stdout", "-s",
                help = "print output")
                .default(false)

        val nofile by parser.flagging("--nofile", "-n",
                help = "do not write output to a file")
                .default(false)
    }
}