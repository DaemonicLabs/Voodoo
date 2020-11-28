package voodoo.pack

import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import voodoo.data.DependencyType
import voodoo.data.Side
import voodoo.data.lock.LockPack
import voodoo.mmc.MMCSelectable
import voodoo.mmc.MMCUtil
import voodoo.provider.Providers
import voodoo.util.Directories
import voodoo.util.blankOr
import voodoo.util.packToZip
import voodoo.util.withPool
import java.io.File

object MMCFatPack : AbstractPack("mmc-fat") {
    override val label = "MultiMC Pack (frozen pack)"

    override fun File.getOutputFolder(id: String, version: String): File = resolve("multimc-fat")

    override suspend fun pack(
        stopwatch: Stopwatch,
        modpack: LockPack,
        output: File,
        uploadBaseDir: File,
        clean: Boolean
    ) = stopwatch {
        val directories = Directories.get()

        val cacheDir = directories.cacheHome
        val zipRootDir = cacheDir.resolve("MMC_FAT").resolve(modpack.id).resolve(modpack.version)
        val instanceDir = zipRootDir.resolve(modpack.id)
        val title = modpack.title.blankOr ?: modpack.id
        zipRootDir.deleteRecursively()

        instanceDir.mkdirs()

        val minecraftDir = MMCUtil.installEmptyPack(
            title.blankOr,
            modpack.id,
            icon = modpack.iconFile,
            instanceDir = instanceDir,
            mcVersion = modpack.mcVersion,
            modloader = modpack.modloader,
            extraCfg = modpack.packOptions.multimcOptions.instanceCfg
//            forgeVersion = ForgeUtil.forgeVersionOf(modpack.forge)?.forgeVersion
        )

        minecraftDir.mkdirs()
        val modsDir = minecraftDir.resolve("mods")
        modsDir.deleteRecursively()

        logger.info("copying files into minecraft dir")
        val minecraftSrcDir = modpack.sourceFolder
        if (minecraftSrcDir.exists()) {
            minecraftSrcDir.copyRecursively(minecraftDir, overwrite = true)
        }

        for (file in minecraftDir.walkTopDown()) {
            when (file.name) {
                "_CLIENT" -> {
                    file.copyRecursively(file.parentFile, overwrite = true)
                    file.deleteRecursively()
                }
                "_SERVER" -> {
                    file.deleteRecursively()
                }
            }
        }

        val json = Json {
            prettyPrint = true
            encodeDefaults = false
        }

        // read user input
        val featureJson = instanceDir.resolve("voodoo.features.json")
        val previousSelection = if (featureJson.exists()) {
            json.decodeFromString(MapSerializer(String.serializer(), Boolean.serializer()), featureJson.readText())
        } else {
            mapOf<String, Boolean>()
        }
        val (optionals, reinstall) = MMCUtil.updateAndSelectFeatures(
            modpack.optionalEntries.filterValues { it.side != Side.SERVER }.map { (id, entry) ->
                MMCSelectable(id, entry)
            },
            previousSelection,
            modpack.title.blankOr
                ?: modpack.id, modpack.version,
            enableTimeout = false,
            installing = true,
            updateRequired = true
        )
        logger.debug("result: optionals: $optionals")
        if (!optionals.isEmpty()) {
            featureJson.createNewFile()
            featureJson.writeText(json.encodeToString(MapSerializer(String.serializer(), Boolean.serializer()), optionals))
        }
        if (reinstall) {
            minecraftDir.deleteRecursively()
        }

        withPool { pool ->
            coroutineScope {
                for ((entryId, entry) in modpack.entries) {
                    if (entry.side == Side.SERVER) continue

                    launch(context = coroutineContext + pool) {
                        val folder = minecraftDir.resolve(entry.path).absoluteFile

                        val matchedOptioalsList = if(modpack.isEntryOptional(entryId)) {
                            val selectedSelf = optionals[entryId] ?: true
                            if (!selectedSelf) {
                                MMCUtil.logger.info("${entry.displayName(entryId)} is disabled, skipping download")
                                return@launch
                            }
                            modpack.optionalEntries.filter { (optionalEntryId, optionalEntry) ->
                                // check if entry is a dependency of any feature
                                modpack.isDependencyOf(
                                    entryId = entryId,
                                    parentId = optionalEntryId,
                                    dependencyType = DependencyType.REQUIRED
                                )
                            }
                        } else emptyMap()
                        val provider = Providers[entry.providerType]
                        val targetFolder = minecraftDir.resolve(folder)
                        val (_, file) = provider.download(
                            "download-${entryId}".watch,
                            entryId,
                            entry,
                            targetFolder,
                            cacheDir
                        ) ?: return@launch

                        if (matchedOptioalsList.isNotEmpty()) {
                            val selected = matchedOptioalsList.any { (id, entry) -> optionals[id] ?: false }
                            if (!selected) {
                                MMCUtil.logger.info("${matchedOptioalsList.map { (id, entry) -> entry.displayName(id) }} is disabled, disabling $entryId")
                                file.renameTo(file.parentFile.resolve(file.name + ".disabled"))
                            }
                        }
                    }
                }

                delay(10)
                CursePack.logger.info("waiting for jobs to finish")
            }
        }

        logger.info { "clearing serverside files and deleting lockfiles" }
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

        output.mkdirs()
        val instanceZip = output.resolve("${modpack.id}-${modpack.version}.zip")

        instanceZip.delete()
        packToZip(zipRootDir, instanceZip)
        logger.info("created mmc pack $instanceZip")
    }
}