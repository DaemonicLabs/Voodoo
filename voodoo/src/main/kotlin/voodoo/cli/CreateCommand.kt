package voodoo.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.data.ModloaderPattern
import voodoo.data.Side
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.config.generateSchema
import java.io.File

class CreateCommand : CliktCommand(
    name = "create",
    help = "create a new pack"
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    val cliContext by requireObject<CLIContext>()

    val id by option(
        "--id",
        help = "pack id"
    ).required()

    val mcVersion by option(
        "--mcVersion",
        help = "minecraft version"
    )

    val title by option(
        "--title",
        help = "modpack title"
    )

    override fun run(): Unit = withLoggingContext("command" to commandName){
        val rootDir = cliContext.rootDir
        val packFile = rootDir.resolve("$id.voodoo.json")

        require(!packFile.exists()) {
            "file $packFile already exists"
        }

        val nestedPack = NestedPack(
            schema = "./schema/nested_modpack.schema.json",
            mcVersion = mcVersion,
            title = title,
            version = "0.0.1",
            icon = "icon_$id.png",
            authors = listOf(""),
            modloader = ModloaderPattern.None,
            root = NestedEntry.Common(
                entries = mapOf(
                    "common" to NestedEntry.Common().apply {
                        side = Side.BOTH
                    },
                    "clientside" to NestedEntry.Common().apply {
                        side = Side.CLIENT
                    },
                    "serverside" to NestedEntry.Common().apply {
                        side = Side.SERVER
                    }
                )
            )
        )

        File(nestedPack.schema).apply {
            absoluteFile.parentFile.mkdirs()
            writeText(NestedPack.generateSchema())
        }
    }

}