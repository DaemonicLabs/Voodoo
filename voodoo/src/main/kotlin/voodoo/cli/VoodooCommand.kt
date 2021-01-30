package voodoo.cli

import ch.qos.logback.classic.Level
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import mu.KotlinLogging
import mu.withLoggingContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import voodoo.cli.init.InitCommand
import voodoo.cli.launch.LaunchCommand
import voodoo.util.SharedFolders
import voodoo.voodoo.GeneratedConstants
import java.io.File

class VoodooCommand(invocation: String = "voodoo") : CliktCommand(
    name = invocation,
    help = "modpack building magic",
//    allowMultipleSubcommands = true
//    invokeWithoutSubcommand = true
) {
    private val logger = KotlinLogging.logger {}
    init {
        versionOption(GeneratedConstants.FULL_VERSION)
        subcommands(
            InitCommand(),
            CompileCommand(),
            ChangelogCommand(),
            PackageCommand(),
            LaunchCommand(),
            GenerateSchemaCommand(),
            UpdateCommand(),
            ImportCurseCommand()
        )
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
        .choice(
            listOf(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR).associateBy { it.levelStr },
            ignoreCase = true
        )
        .default(Level.INFO)

    val cliContext by lazy {
        logger.debug { "creating CLI context" }
        CLIContext(
            rootDir = rootDir
        )
    }

//    val rootDir: File by findOrSetObject { rootDir }

    override fun run(): Unit = withLoggingContext("command" to commandName) {
        logger.info { "running voodoo" }

        // setting up CLI context
        currentContext.obj = cliContext

        logger.info { "context: $cliContext" }

        // setting loglevel
        logger.info { "setting loglevel = $logLevel" }
        val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        rootLogger.level = logLevel

        // initializing SharedFolders
        // TODO: if possible remove shared folders
        if(!SharedFolders.RootDir.defaultInitialized) {
            SharedFolders.RootDir.value = rootDir.absoluteFile
        }
    }
}