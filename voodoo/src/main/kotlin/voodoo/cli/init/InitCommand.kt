package voodoo.cli.init

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.cli.CLIContext
import voodoo.util.maven.MavenUtil
import voodoo.voodoo.GeneratedConstants
import java.io.StringWriter
import java.util.*

class InitCommand : CliktCommand(
    name = "init",
    help = "make new things",
) {
    private val logger = KotlinLogging.logger {}
    val cliContext by requireObject<CLIContext>()

    init {
        subcommands(
            InitPackCommand(),
            InitProjectCommand()
        )
    }

    override fun run(): Unit = withLoggingContext("command" to commandName) {

    }
}