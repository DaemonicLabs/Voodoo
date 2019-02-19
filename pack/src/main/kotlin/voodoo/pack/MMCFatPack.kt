package voodoo.pack

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.internal.BooleanSerializer
import kotlinx.serialization.internal.HashMapSerializer
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.map
import kotlinx.serialization.serializer
import voodoo.data.Side
import voodoo.data.curse.DependencyType
import voodoo.data.lock.LockPack
import voodoo.forge.ForgeUtil
import voodoo.mmc.MMCSelectable
import voodoo.mmc.MMCUtil
import voodoo.provider.Providers
import voodoo.util.blankOr
import voodoo.util.packToZip
import voodoo.util.withPool
import java.io.File

object MMCFatPack : AbstractPack() {
    override val label = "MultiMC Pack (frozen pack)"

    override fun File.getOutputFolder(id: String): File = resolve("multimc-fat")

    override suspend fun pack(
        modpack: LockPack,
        output: File,
        clean: Boolean
    ) {
        val cacheDir = directories.cacheHome
        val instanceDir = cacheDir.resolve("MMC_FAT").resolve(modpack.id)
        val title = modpack.title.blankOr ?: modpack.id
        instanceDir.deleteRecursively()

        instanceDir.mkdirs()

        val minecraftDir = MMCUtil.installEmptyPack(
            title,
            modpack.id,
            icon = modpack.icon,
            instanceDir = instanceDir,
            mcVersion = modpack.mcVersion,
            forgeVersion = ForgeUtil.forgeVersionOf(modpack.forge)?.forgeVersion
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

        val json = Json(indented = true, encodeDefaults = false)

        // read user input
        val featureJson = instanceDir.resolve("voodoo.features.json")
        val previousSelection = if (featureJson.exists()) {
            json.parse(HashMapSerializer(String.serializer(), BooleanSerializer), featureJson.readText())
        } else {
            mapOf<String, Boolean>()
        }
        val (optionals, reinstall) = MMCUtil.selectFeatures(
            modpack.optionalEntries.map {
                MMCSelectable(it)
            },
            previousSelection,
            modpack.title.blankOr
                ?: modpack.id, modpack.version, forceDisplay = false, updating = featureJson.exists()
        )
        logger.debug("result: optionals: $optionals")
        if (!optionals.isEmpty()) {
            featureJson.createNewFile()
            featureJson.writeText(json.stringify((StringSerializer to BooleanSerializer).map, optionals))
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

                        if(modpack.isEntryOptional(entry.id)) {
                            val selectedSelf = optionals[entry.id] ?: true
                            if (!selectedSelf) {
                                MMCUtil.logger.info("${entry.displayName} is disabled, skipping download")
                                return@launch
                            }
                            val matchedOptioalsList = modpack.optionalEntries.filter {
                                // check if entry is a dependency of any feature
                                modpack.isDependencyOf(
                                    entryId = entry.id,
                                    parentId = it.id,
                                    dependencyType = DependencyType.REQUIRED
                                )
                            }

                            val provider = Providers[entry.provider]
                            val targetFolder = minecraftDir.resolve(folder)
                            val (_, file) = provider.download(entry, targetFolder, cacheDir)

                            if (!matchedOptioalsList.isEmpty()) {
                                val selected = matchedOptioalsList.any { optionals[it.id] ?: false }
                                if (!selected) {
                                    MMCUtil.logger.info("${matchedOptioalsList.map { it.displayName }} is disabled, disabling ${entry.id}")
                                    file.renameTo(file.parentFile.resolve(file.name + ".disabled"))
                                }
                            }
                        }

                    }
                }

                delay(10)
                CursePack.logger.info("waiting for jobs to finish")
            }
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

        output.mkdirs()
        val instanceZip = output.resolve(modpack.id + ".zip")

        instanceZip.delete()
        packToZip(instanceDir.toPath(), instanceZip.toPath())
        logger.info("created mmc pack $instanceZip")
    }
}