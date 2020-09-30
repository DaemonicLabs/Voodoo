import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import java.io.File

class BuildCommand : CliktCommand(name = "build") {
    val packId by argument("PACK")

    override fun run() {
        val packConfig = File("$packId.voodoo.txt")


    }
}