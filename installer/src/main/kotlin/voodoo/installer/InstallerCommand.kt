package voodoo.installer

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class InstallerCommand :CliktCommand(
    name = "installer",
){
    init {
        subcommands(
            InstallMultiMCCommand(),
            InstallGenericCommand()
        )
    }
    override fun run() {

    }
}