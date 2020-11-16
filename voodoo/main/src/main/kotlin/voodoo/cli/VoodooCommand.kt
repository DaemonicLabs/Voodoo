package voodoo.cli

import ch.qos.logback.classic.Level
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.file
import mu.KotlinLogging
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import voodoo.util.Directories
import voodoo.util.SharedFolders
import voodoo.voodoo.main.GeneratedConstants
import java.io.File

class VoodooCommand : CliktCommand(
//    name = "Voodoo",
    help = "",
//    allowMultipleSubcommands = true
//    invokeWithoutSubcommand = true
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    init {
        versionOption(GeneratedConstants.FULL_VERSION)
    }

    val rootDir by option("--rootDir",
        help = "root directory for voodoo execution")
        .file(mustExist = true, canBeFile = false, canBeDir = true, mustBeWritable = true)
        .default(
            File(System.getProperty("user.dir")).absoluteFile
        )

    private val logLevel: Level by option(
        "--log",
        help = "set loglevel"
        )
        .convert {
            Level.valueOf(it)
        }
        .default(Level.INFO)

    init {
        subcommands(
            EvalScriptCommand(),
            BuildCommand(),
            ChangelogCommand()
        )
    }

//    val rootDir: File by findOrSetObject { rootDir }

    override fun run() {
        logger.info { "running voodoo" }
        currentContext.obj = rootDir
        val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        rootLogger.level = logLevel

        if(!SharedFolders.RootDir.defaultInitialized) {
            SharedFolders.RootDir.value = rootDir.absoluteFile
        }
    }
}