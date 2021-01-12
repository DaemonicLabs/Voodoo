package voodoo.cli.launch

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import mu.KotlinLogging
import mu.withLoggingContext

class LaunchCommand(): CliktCommand(
    name = "launch",
//    help = ""
) {
    private val logger = KotlinLogging.logger {}

    init {
        subcommands(LaunchMultiMCCommand())
    }

    override fun run() = withLoggingContext("command" to commandName) {

    }
}