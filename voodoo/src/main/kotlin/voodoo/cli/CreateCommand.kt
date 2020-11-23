package voodoo.cli

import com.eyeem.watchadoin.Stopwatch
import com.eyeem.watchadoin.saveAsHtml
import com.eyeem.watchadoin.saveAsSvg
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.cli.create.CreatePackCommand
import voodoo.cli.create.CreateVersionCommand

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
            CreateVersionCommand()
        )
    }

    override fun run(): Unit = withLoggingContext("command" to commandName) {

    }

}