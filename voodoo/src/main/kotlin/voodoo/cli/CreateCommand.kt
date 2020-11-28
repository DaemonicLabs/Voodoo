package voodoo.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.cli.create.CreatePackCommand

class CreateCommand : CliktCommand(
    name = "create",
    help = "creates things"
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    init {
        subcommands(
            CreatePackCommand(),
//            CreateVersionCommand(),
        )
    }

    override fun run(): Unit = withLoggingContext("command" to commandName) {

    }

}