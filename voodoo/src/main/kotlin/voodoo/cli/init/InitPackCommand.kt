package voodoo.cli.init

import com.eyeem.watchadoin.Stopwatch
import com.eyeem.watchadoin.saveAsHtml
import com.eyeem.watchadoin.saveAsSvg
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.cli.CLIContext
import voodoo.pack.*
import voodoo.util.json
import java.io.File

class InitPackCommand : CliktCommand(
    name = "pack",
    help = "create a new pack"
) {
    private val logger = KotlinLogging.logger {}
    val cliContext by requireObject<CLIContext>()

    val id by option(
        "--id",
        help = "pack id"
    ).required()
        .validate {
            require(it.isNotBlank()) { "id must not be blank" }
            require(it.matches("""[\w_]+""".toRegex())) { "modpack id must not contain special characters" }
        }

    val mcVersion by option(
        "--mcVersion",
        help = "minecraft version"
    ).required()

    val title by option(
        "--title",
        help = "modpack title"
    )

    val packVersion by option(
        "--packVersion",
        help = "pack version"
    ).validate {  version ->
        require(version.matches("^\\d+(?:\\.\\d+)+$".toRegex())) {
            "version must match pattern '^\\d+(\\.\\d+)+\$' eg: 0.1 or 4.11.6 or 1.2.3.4 "
        }
    }

    override fun run(): Unit = withLoggingContext("command" to commandName) {
        runBlocking(MDCContext()) {

            val rootDir = cliContext.rootDir
            val stopwatch = Stopwatch(commandName)

            stopwatch {
                val metaPack = MetaPack(
                    title = title,
                    icon = "icon_$id.png",
                    authors = listOf(
                        System.getProperty("user.name")
                    ),
                    uploadBaseUrl = "https://mydomain.com/mc/",
                )

                val baseDir = rootDir.resolve(id)

                val versionPack = VersionPack(
                        mcVersion = mcVersion,
                        title = title,
                        version = packVersion ?: "0.0.1",
                        srcDir = "v${packVersion ?: "0.0.1"}_src",
                        modloader = Modloader.None,
                        packageConfiguration = VersionPackageConfig(),
                        mods = mapOf(
                            //TODO: add mod samples there
                        )
                    ).postParse(baseDir = baseDir)

                val flatModpack = versionPack.flatten(rootDir, id, metaPack, mapOf())

                // create folders
                require(!baseDir.exists() || (baseDir.isDirectory && baseDir.list()!!.isEmpty())) { "folder $baseDir must not exist or be a empty directory" }
                baseDir.mkdirs()

                val srcFolder = flatModpack.sourceFolder
                require(!srcFolder.exists() || (srcFolder.isDirectory && srcFolder.list()!!.isEmpty())) { "folder $srcFolder must not exist or be a empty directory" }
                srcFolder.mkdirs()

                // write metaPack
                val metapackFile = baseDir.resolve(MetaPack.FILENAME)
                val savedTo = metaPack.save(baseDir = baseDir)
                metapackFile.writeText(json.encodeToString(MetaPack.serializer(), metaPack))

                // write versionPack
                baseDir.resolve("v${versionPack.version}.${VersionPack.extension}").also { file ->
                    file.writeText(json.encodeToString(VersionPack.serializer(), versionPack))
                }

                logger.info { "created pack $savedTo" }
            }
            val reportDir = File("reports").apply { mkdirs() }
            stopwatch.saveAsSvg(reportDir.resolve("${id}_build.report.svg"))
            stopwatch.saveAsHtml(reportDir.resolve("${id}_build.report.html"))
        }

    }

}