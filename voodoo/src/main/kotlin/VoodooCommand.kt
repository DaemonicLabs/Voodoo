import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import mu.KotlinLogging

class VoodooCommand : CliktCommand(
//    allowMultipleSubcommands = true
) {
    private val logger = KotlinLogging.logger {}
    init {
        subcommands(
            BuildCommand()
        )
    }

    override fun run() {

    }
}