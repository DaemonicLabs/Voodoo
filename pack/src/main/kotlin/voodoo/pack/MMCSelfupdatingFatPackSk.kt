package voodoo.pack

import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import moe.nikky.voodoo.format.modpack.Manifest
import voodoo.data.DependencyType
import voodoo.data.Side
import voodoo.data.lock.LockPack
import voodoo.mmc.MMCSelectable
import voodoo.mmc.MMCUtil
import voodoo.provider.Providers
import voodoo.util.*
import voodoo.util.maven.MavenUtil
import java.io.File
import java.net.URI
import kotlin.system.exitProcess

object MMCSelfupdatingFatPackSk : AbstractPack("mmc-sk-fat") {
    override val label = "MultiMC Pack"

    override fun File.getOutputFolder(id: String): File = resolve("multimc-sk-fat")

    override suspend fun pack(
        stopwatch: Stopwatch,
        modpack: LockPack,
        output: File,
        uploadBaseDir: File,
        clean: Boolean
    ) = stopwatch {
        val cacheDir = directories.cacheHome
        val zipRootDir = cacheDir.resolve("MMC_SK_FAT").resolve(modpack.id)
        val instanceDir = zipRootDir.resolve(modpack.id)
        val title = modpack.title.blankOr ?: modpack.id
        zipRootDir.deleteRecursively()

        instanceDir.mkdirs()

        val installerFilename = "mmc-installer.jar"
        val preLaunchCommand =
            "\"\$INST_JAVA\" -jar \"\$INST_DIR/$installerFilename\" --id \"\$INST_ID\" --inst \"\$INST_DIR\" --mc \"\$INST_MC_DIR\""
        val minecraftDir = MMCUtil.installEmptyPack(
            title.blankOr,
            modpack.id,
            icon = modpack.iconFile,
            mcVersion = modpack.mcVersion,
            modloader = modpack.modloader,
            extraCfg = modpack.packOptions.multimcOptions.instanceCfg,
            instanceDir = instanceDir,
            preLaunchCommand = preLaunchCommand
        )

        minecraftDir.mkdirs()
        val modsDir = minecraftDir.resolve("mods")
        modsDir.deleteRecursively()

        MMCFatPack.logger.info("copying files into minecraft dir")
        val minecraftSrcDir = modpack.sourceFolder
        if (minecraftSrcDir.exists()) {
            minecraftSrcDir.copyRecursively(minecraftDir, overwrite = true)
        }

        for (file in minecraftDir.walkTopDown()) {
            when {
                file.name == "_CLIENT" -> {
                    file.copyRecursively(file.parentFile, overwrite = true)
                    file.deleteRecursively()
                }
                file.name == "_SERVER" -> {
                    file.deleteRecursively()
                }
            }
        }

        val json = Json(JsonConfiguration(prettyPrint = true, encodeDefaults = false))

        // read user input
        val featureJson = instanceDir.resolve("voodoo.features.json")
        val previousSelection = if (featureJson.exists()) {
            json.parse(MapSerializer(String.serializer(), Boolean.serializer()), featureJson.readText())
        } else {
            mapOf<String, Boolean>()
        }
        val (optionals, reinstall) = MMCUtil.updateAndSelectFeatures(
            modpack.optionalEntries.filter{ it.side != Side.SERVER }.map {
                MMCSelectable(it)
            },
            previousSelection,
            name = modpack.title.blankOr ?: modpack.id,
            version = modpack.version,
            enableTimeout = false,
            installing = true,
            updateRequired = true
        )
        MMCFatPack.logger.debug("result: optionals: $optionals")
        if (!optionals.isEmpty()) {
            featureJson.createNewFile()
            featureJson.writeText(json.stringify(MapSerializer(String.serializer(), Boolean.serializer()), optionals))
        }
        if (reinstall) {
            minecraftDir.deleteRecursively()
        }

        withPool { pool ->
            coroutineScope {
                for (entry in modpack.entrySet) {
                    if (entry.side == Side.SERVER) continue

                    launch(context = coroutineContext + pool) {
                        val folder = minecraftDir.resolve(entry.serialFile).absoluteFile.parentFile

                        val matchedOptioalsList = if (modpack.isEntryOptional(entry.id)) {
                            val selectedSelf = optionals[entry.id] ?: true
                            if (!selectedSelf) {
                                MMCUtil.logger.info("${entry.displayName} is disabled, skipping download")
                                return@launch
                            }
                            modpack.optionalEntries.filter {
                                // check if entry is a dependency of any feature
                                modpack.isDependencyOf(
                                    entryId = entry.id,
                                    parentId = it.id,
                                    dependencyType = DependencyType.REQUIRED
                                )
                            }
                        } else emptyList()
                        val provider = Providers[entry.provider]
                        val targetFolder = minecraftDir.resolve(folder)
                        val (_, file) = provider.download(
                            "download-${entry.id}".watch,
                            entry,
                            targetFolder,
                            cacheDir
                        ) ?: return@launch

                        if (!matchedOptioalsList.isEmpty()) {
                            val selected = matchedOptioalsList.any { optionals[it.id] ?: false }
                            if (!selected) {
                                MMCUtil.logger.info("${matchedOptioalsList.map { it.displayName }} is disabled, disabling ${entry.id}")
                                file.renameTo(file.parentFile.resolve(file.name + ".disabled"))
                            }
                        }
                    }
                }

                delay(10)
                CursePack.logger.info("waiting for jobs to finish")
            }
        }

        MMCFatPack.logger.info("clearing serverside files and deleting lockfiles")
        for (file in minecraftDir.walkTopDown()) {
            when {
                file.name.endsWith(".lock.json") -> file.delete()
                file.name == "_CLIENT" -> {
                    file.copyRecursively(file.parentFile, overwrite = true)
                    file.deleteRecursively()
                }
                file.name == "_SERVER" -> {
                    file.deleteRecursively()
                }
            }
        }


        val skPackUrl = modpack.packOptions.multimcOptions.skPackUrl
            ?: run {
                modpack.packOptions.baseUrl?.let { baseUrl ->
                    val skOutput = with(SKPack) { uploadBaseDir.getOutputFolder(modpack.id) }
                    val skPackFile = skOutput.resolve("${modpack.id}.json")
                    val relativePath = skPackFile.relativeTo(uploadBaseDir).unixPath
                    URI(baseUrl).resolve(relativePath).toASCIIString()
                }
            }
        if (skPackUrl == null) {
            MMCSelfupdatingPackSk.logger.error("skPackUrl in multimc options is not set")
            exitProcess(3)
        }
        val urlFile = instanceDir.resolve("voodoo.url.txt")
        urlFile.writeText(skPackUrl)

        // preinstall the voodoo.modpack.json
        val voodooModpackJson = instanceDir.resolve("voodoo.modpack.json")
        voodooModpackJson.download(
            skPackUrl,
            cacheDir = null // TODO: use null here to signal not to use cache ?
//            validator = { false }
        )
        json.parse(Manifest.serializer(), voodooModpackJson.readText())

        val multimcInstaller = instanceDir.resolve(installerFilename)
        val installer = "downloadArtifact multimc installer bootstrap".watch {
            MavenUtil.downloadArtifact(
                mavenUrl = GeneratedConstants.MAVEN_URL,
                group = GeneratedConstants.MAVEN_GROUP,
                artifactId = "bootstrap-multimc-installer",
                version = ModuleBootstrapMultimcInstaller.FULL_VERSION,
                classifier = GeneratedConstants.MAVEN_SHADOW_CLASSIFIER,
                outputFile = multimcInstaller,
                outputDir = directories.cacheHome
            )
        }

//        installer.copyTo(multimcInstaller)

        val packignore = instanceDir.resolve(".packignore")
        packignore.writeText(
            """.minecraft
                  |mmc-pack.json
                """.trimMargin()
        )

        output.mkdirs()
        val instanceZip = output.resolve(modpack.id + ".zip")

        instanceZip.delete()
        packToZip(zipRootDir, instanceZip)
        MMCFatPack.logger.info("created mmc pack $instanceZip")
    }
}