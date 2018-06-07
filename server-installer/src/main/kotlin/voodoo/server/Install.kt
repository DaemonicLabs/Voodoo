package voodoo.server

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.util.readJson
import java.io.File

/**
 * Created by nikky on 11/05/18.
 * @author Nikky
 */

object Install : KLogging() {

    @JvmStatic
    fun main(vararg args: String): Unit = mainBody {

        val parser = ArgParser(args)
        val parsedArgs = Arguments(parser)
        parser.force()

        parsedArgs.run {
            logger.info ("target dir: $targetDir")
            logger.info ("pack file: $packFile")
            logger.info("dummy code, please implement")

            val modpack = packFile.readJson<LockPack>()

            Server.install(modpack, targetDir, false)
        }
    }

    private class Arguments(parser: ArgParser) {
        val targetDir by parser.positional("TARGET",
                help = "output folder") { File(this) }
                .addValidator {
                    if(value.exists() && !value.isDirectory) {
                        throw InvalidArgumentException("$value exists and is not a directory")
                    }
                }

        val packFile by parser.storing("--file", "-f",
                help = "input pack lock.json") { File(this) }
                .default(File("pack.lock.json"))

    }
}
