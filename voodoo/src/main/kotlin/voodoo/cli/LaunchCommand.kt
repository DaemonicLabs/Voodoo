package voodoo.cli

import com.eyeem.watchadoin.Stopwatch
import com.eyeem.watchadoin.saveAsHtml
import com.eyeem.watchadoin.saveAsSvg
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.Pack
import voodoo.cli.launch.MultiMCCommand
import voodoo.data.lock.LockPack
import voodoo.pack.AbstractPack
import voodoo.util.SharedFolders
import java.io.File

class LaunchCommand(): CliktCommand(
    name = "launch",
//    help = ""
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    init {
        subcommands(MultiMCCommand())
    }

    override fun run() = withLoggingContext("command" to commandName) {

    }
}