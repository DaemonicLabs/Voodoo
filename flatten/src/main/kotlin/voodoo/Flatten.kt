package voodoo

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import mu.KotlinLogging
import voodoo.core.data.nested.NestedPack
import voodoo.util.json
import voodoo.util.readYaml
import voodoo.util.writeJson
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 * @version 1.0
 */

private val logger = KotlinLogging.logger {}
fun main(vararg args: String) = mainBody {

    val arguments = Arguments(ArgParser(args))

    arguments.run {
        val inFile = File(importArg)
        println(inFile.absolutePath)
        val nestedPack = inFile.readYaml<NestedPack>()
        var target = targetArg


        logger.info("FLATTENING...")
        val flatpack = nestedPack.flatten()

        if (stdout) {
            print(flatpack.json)
        } else {
            if (target.isEmpty()) target = "${nestedPack.name}.json"
            if (!target.endsWith(".json")) target += ".json"
            val targetFile = File(target)
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
}