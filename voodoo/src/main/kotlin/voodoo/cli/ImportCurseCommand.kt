package voodoo.cli

import com.eyeem.watchadoin.Stopwatch
import com.eyeem.watchadoin.saveAsHtml
import com.eyeem.watchadoin.saveAsSvg
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.config.Autocompletions
import voodoo.config.Configuration
import voodoo.curse.CurseClient
import voodoo.data.curse.CurseFile
import voodoo.data.curse.CurseManifest
import voodoo.data.curse.FileType
import voodoo.pack.*
import voodoo.poet.generator.CurseSection
import voodoo.util.*
import java.io.File

class ImportCurseCommand() : CliktCommand(
    name = "importCurse",
    help = "imports a pack from curse"
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

    val pinFiles by option(
        "--pinFiles",
        help = "pin file versions"
    ).flag(default = false)

    val uri: String by argument("CURSE_PACK")

    override fun run() = withLoggingContext("command" to commandName) {
        val directories = Directories.get(moduleName = "CURSE")
        val cacheHome by lazy {
            directories.cacheHome.resolve(id).apply {
                mkdirs()
            }
        }

        runBlocking(MDCContext()) {
            val stopwatch = Stopwatch(commandName)

            val rootDir = cliContext.rootDir

            val jsonPretty = Json(json) {
                prettyPrint = true
                encodeDefaults = false
            }

            stopwatch {
                val importFile = cacheHome.resolve("file.zip")
                // TODO: download or load from file
                if(uri.startsWith("http")) {
                    importFile.download(uri, cacheDir = null)
                } else {
                    File(uri).copyTo(importFile)
                }

                logger.info { "downloaded: $importFile" }

                // TODO: unzip
                val importFolder = cacheHome.resolve("unzipped")
                importFolder.deleteRecursively()
                UnzipUtility.unzip(importFile, importFolder)

                // TODO: parse curse manifest
                val manifest = json.decodeFromString(CurseManifest.serializer(), importFolder.resolve("manifest.json").readText())

                val iconFile = importFolder.resolve(manifest.overrides).resolve("icon.png")

//                val modpackAddon = CurseClient.getAddon(ProjectID(manifest.projectID)) ?: error("cannot load addon modpack")

                logger.info { jsonPretty.encodeToString(CurseManifest.serializer(), manifest) }

                val baseDir = rootDir.resolve(id)

                // convert to meta pack
                val metaPackFile = baseDir.resolve(MetaPack.FILENAME)
                metaPackFile.absoluteFile.parentFile.mkdirs()
                val metaPack = MetaPack(
                    title = manifest.name,
                    authors = listOf(manifest.author).filter { it.isNotBlank() },
//                        authors = modpackAddon.authors.map { author ->
//                            author.name
//                        },
//                        icon = "icon.png",
                    uploadBaseUrl = "https://mydomain.com/mc/",
                )


                // convert to version pack
                val version = manifest.version
                val cleanVersion = version //.replace(".", "_") // TODO: cleanup of invalid chars
                val srcFolder = baseDir.resolve("src_$cleanVersion")

                val versionPackFile = baseDir.resolve("${cleanVersion}.voodoo.json5")

                // ensure no version conflict is created
                if(versionPackFile.exists()) {
                    error("version already exists: $versionPackFile")
                }
                if(srcFolder.exists() && srcFolder.list()?.isEmpty() == false) {
                    error("folder $srcFolder already exists")
                }

                val curseModloader = manifest.minecraft.modLoaders.first { it.primary == true }
                val modloader = if(curseModloader.id.startsWith("forge-")) {
                    val forgeVersion = manifest.minecraft.version + "-" + curseModloader.id.substringAfter("forge-")
                    val forgeAlias = Autocompletions.forge.entries.firstOrNull { (key, value) ->
                        value == forgeVersion
                    }?.key ?: run {
                        logger.error { "cannot find forge alias for $forgeVersion in autocompletions, make sure to add it in ${Configuration.CONFIG_PATH}" }
                        forgeVersion
                    }
                    Modloader.Forge(version = forgeAlias)
                } else {
                    error("unknown modloader $curseModloader")
                }

                val mods = withPool { pool ->
                    manifest.files.map { curseFileEntry: CurseFile ->
                        async(CoroutineName("processing_${curseFileEntry.projectID}") + pool) {
                            logger.info { "processing: $curseFileEntry" }

                            // get curseforge info
                            val addon = CurseClient.getAddon(addonId = curseFileEntry.projectID) ?: error("cannot get addon")
                            val addonFile = CurseClient.getAddonFile(addonId = curseFileEntry.projectID, fileId = curseFileEntry.fileID) ?: error("cannot get addon file")

                            val projectName = Autocompletions.curseforge.entries.firstOrNull { (key, projectIdString) ->
                                projectIdString.toInt() == curseFileEntry.projectID.value
                            }?.key ?: run {
                                logger.error { "cannot find $curseFileEntry in autocompletions, make sure to add it in ${Configuration.CONFIG_PATH}" }
                                "add_me/" + addon.slug
                            }
                            var hasCustomProperties = false

                            var entry = FileEntry.Curse(
                                curse_projectName = projectName,
                            )

//                            // TODO: handle this properly
//                            entry.invalidMcVersions += "Fabric"
//                            hasCustomProperties = true

                            if(pinFiles) {
                                entry = entry.copy(
                                    curse_fileID = curseFileEntry.fileID
                                )
                                hasCustomProperties = true
                            }

                            if(addonFile.releaseType != FileType.Release) {
                                entry = entry.copy(
                                    curse_releaseTypes = entry.curse_releaseTypes + addonFile.releaseType
                                )
                                hasCustomProperties = true
                            }
                            if(!addonFile.gameVersion.contains(manifest.minecraft.version)) {
                                entry = entry.copy(
                                    validMcVersions = addonFile.gameVersion.toSet() - "Forge"
                                )
                                hasCustomProperties = true
                            }
                            when (addon.categorySection.name) {
                                CurseSection.MODS.sectionName -> {
//                                    entry = entry.copy(
//                                        folder = "mods"
//                                    )
                                }
                                CurseSection.RESOURCE_PACKS.sectionName -> {
                                    entry = entry.copy(
                                        folder = "resourcepacks"
                                    )
                                    hasCustomProperties = true
                                }
                                else -> {
                                    error("unknown category type: ${addon.categorySection}")
                                }
                            }
//                                fileNameRegex = "\\Q${addonFile.fileName}\\E"

                            if(!hasCustomProperties) {
                                // had single line string declaration herei n the past
                                // wo probbably can drop hasCustomProperties flag
                                FileEntry.Curse(
                                    curse_projectName = entry.curse_projectName
                                )
                            } else {
                                entry
                            }
                        }
                    }.awaitAll()
                }

                val targetIconFile = srcFolder.resolve("icon.png")

                val versionPack = VersionPack(
                    title = manifest.name + " v" + version,
                    icon = if(iconFile.exists()) targetIconFile.toRelativeUnixPath(baseDir) else null,
                    version = version,
                    srcDir = srcFolder.toRelativeUnixPath(baseDir),
                    mcVersion = manifest.minecraft.version,
                    modloader = modloader,
                    mods = mapOf("" to mods)
                ).postParse(baseDir = baseDir)

                if(!metaPackFile.exists()) {
                    metaPackFile.writeText(
                        jsonPretty.encodeToString(MetaPack.serializer(), metaPack)
                    )
                }

                versionPackFile.writeText(
                    jsonPretty.encodeToString(VersionPack.serializer(), versionPack)
                )

                // copy configs into src and local
                srcFolder.mkdirs()
                importFolder.resolve(manifest.overrides).copyRecursively(srcFolder)
            }

            val reportDir = rootDir.resolve("reports").apply { mkdirs() }
            stopwatch.saveAsSvg(reportDir.resolve("importCurse.report.svg"))
            stopwatch.saveAsHtml(reportDir.resolve("importCurse.report.html"))
        }
    }
}