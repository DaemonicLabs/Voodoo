package voodoo.server

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import kotlinx.serialization.json.JSON
import mu.KLogging
import voodoo.data.UserFiles
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.data.flat.EntryFeature
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.data.sk.FeatureFiles
import voodoo.data.sk.FeatureProperties
import voodoo.data.sk.Launch
import voodoo.data.sk.SKFeature
import voodoo.util.ExceptionHelper
import voodoo.util.runBlockingWith
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

        parsedArgs.runBlockingWith(context = ExceptionHelper.context) {
            logger.info("target dir: $targetDir")
            logger.info("pack file: $packFile")
            logger.info("cleanConfig: $cleanConfig")

            val modpack: LockPack = JSON.unquoted.parse(packFile.readText())
            val rootFolder = packFile.absoluteFile.parentFile
            modpack.loadEntries(rootFolder)

            Server.install(modpack, targetDir, skipForge, clean, cleanConfig)
        }
    }

    private class Arguments(parser: ArgParser) {
        val targetDir by parser.positional("TARGET",
                help = "output rootFolder") { File(this).absoluteFile }
                .addValidator {
                    if (value.exists() && !value.isDirectory) {
                        throw InvalidArgumentException("$value exists and is not a directory")
                    }
                }

        val packFile by parser.storing("--file", "-f",
                help = "input pack lock.json") { File(this) }
                .default(File("pack.lock.json"))

        val skipForge by parser.flagging("--skipForge",
                help = "do not call the forge installer")
                .default(false)

        val clean by parser.flagging("--clean",
                help = "clean install (WARNING: will delete server contents before install)")
                .default(false)

        val cleanConfig by parser.flagging("--cleanConfig",
                help = "delete all configs before install")
                .default(false)
    }
}
