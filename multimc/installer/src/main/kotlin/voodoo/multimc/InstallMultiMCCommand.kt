package voodoo.multimc

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.withLoggingContext

class InstallMultiMCCommand: CliktCommand(
    name = "multimc-installer"
) {
    val instanceId by option(
        "--id",
        help = "\$INST_ID - ID of the instance"
    ).required()

    val instanceDir by option(
        "--inst",
        help = "\$INST_DIR - absolute path of the instance"
    ).file(canBeDir = true, canBeFile = false)
        .required()

    val minecraftDir by option(
        "--mc",
        help = "\$INST_MC_DIR - absolute path of minecraft"
    ).file(canBeDir = true, canBeFile = false)
        .required()

    val phase by option(
        "--phase",
        help = "loading phase, pre or post"
    ).enum<Phase>(
        ignoreCase = true
    ).required()

    override fun run() = withLoggingContext("command" to commandName, "instance" to instanceId) {
        runBlocking(MDCContext()) {
            Installer.install(instanceId, instanceDir, minecraftDir, phase)
        }
    }
}

enum class Phase {
    PRE, POST
}
