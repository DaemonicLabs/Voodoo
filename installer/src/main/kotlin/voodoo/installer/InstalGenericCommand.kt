package voodoo.installer

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.withLoggingContext

class InstallGenericCommand: CliktCommand(
    name = "installGeneric"
) {
    override fun run() = withLoggingContext("command" to commandName) {
        runBlocking(MDCContext()) {
            GenericInstaller.install()
        }
    }
}
