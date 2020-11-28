package voodoo.server

import com.eyeem.watchadoin.Stopwatch
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.default
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.slf4j.LoggerFactory
import voodoo.data.lock.LockPack
import voodoo.server.installer.GeneratedConstants.VERSION
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 11/05/18.
 * @author Nikky
 */

object Install : KLogging() {
    @JvmStatic
    fun main(vararg args: String): Unit = runBlocking {
        System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_ON)

        Thread.sleep(500) // wait for logger to catch up

        //FIXME: replace with clikt
        val parser = ArgParser(args)
        val parsedArgs = Arguments(parser)
        parser.force()

        val stopwatch = Stopwatch("main")
        stopwatch {
            parsedArgs.run {
                if (version) {
                    logger.info(VERSION)
                    exitProcess(0)
                }

                require(packPath != null) {
                    "--packFile was not set and no file `pack.txt` found"
                }

                val packFile = File(packPath).absoluteFile

                logger.info("target dir: $targetDir")
                logger.info("cleanConfig: $cleanConfig")

                // TODO: load proper rootDir
                val modpack = LockPack.parse(
                    packFile = packFile,
                    baseFolder = packFile.parentFile
                )

                Server.install("install".watch, modpack, targetDir, skipForge, clean, cleanConfig)
            }
            logger.info(stopwatch.toStringPretty())
        }

    }

    private class Arguments(parser: ArgParser) {
        val parentFolder = File(Install::class.java.protectionDomain.codeSource.location.toURI()).parentFile
        val targetDir by parser.positional(
            "TARGET",
            help = "output rootFolder"
        ) { File(this).absoluteFile!! }
            .addValidator {
                if (value.exists() && !value.isDirectory) {
                    throw InvalidArgumentException("$value exists and is not a directory")
                }
            }

        val packPath by parser.storing(
            "--packFile",
            help = "pack id"
        )
            .default(
                parentFolder.resolve("pack.txt").takeIf { it.exists() }?.readText()?.let { path ->
                    parentFolder.resolve(path).path
                }
            )

        val skipForge by parser.flagging(
            "--skipForge",
            help = "do not call the forge installer"
        )
            .default(false)

        val clean by parser.flagging(
            "--clean",
            help = "clean install (WARNING: will delete server contents before install)"
        )
            .default(false)

        val cleanConfig by parser.flagging(
            "--cleanConfig",
            help = "delete all configs before install"
        )
            .default(false)

        val version by parser.flagging(
            "--version",
            help = "report version and quit"
        )
            .default(false)
    }
}
