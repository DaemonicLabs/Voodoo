package voodoo.pack

import kotlinx.coroutines.experimental.*
import kotlinx.serialization.internal.BooleanSerializer
import kotlinx.serialization.internal.HashMapSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.serializer
import voodoo.data.Side
import voodoo.data.lock.LockPack
import voodoo.forge.Forge
import voodoo.mmc.MMCUtil
import voodoo.provider.Providers
import voodoo.util.*
import java.io.File

object MMCFatPack : AbstractPack() {
    override val label = "MultiMC Packer (frozen pack)"

    override suspend fun download(
        modpack: LockPack,
        folder: File,
        target: String?,
        clean: Boolean
    ) {
        val targetDir = folder.resolve(target ?: ".multimc")
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
            forgeVersion = Forge.forgeVersionOf(modpack.forge)?.forgeVersion
        )

        minecraftDir.mkdirs()
        val modsDir = minecraftDir.resolve("mods")
        modsDir.deleteRecursively()

        Downloader.logger.info("copying files into minecraft dir")
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

        val json = JSON(indented = true)

        // read user input
        val featureJson = instanceDir.resolve("voodoo.features.json")
        val previousSelection = if (featureJson.exists()) {
            json.parse(HashMapSerializer(String.serializer(), BooleanSerializer), featureJson.readText())
        } else {
            mapOf<String, Boolean>()
        }
        val (features, reinstall) = MMCUtil.selectFeatures(
            modpack.features.map { it.feature }, previousSelection,
            modpack.title.blankOr
                ?: modpack.id, modpack.version, forceDisplay = false, updating = featureJson.exists()
        )
        logger.debug("result: features: $features")
        if (!features.isEmpty()) {
            featureJson.createNewFile()
            featureJson.writeText(json.stringify(features))
        }
        if (reinstall) {
            minecraftDir.deleteRecursively()
        }

        coroutineScope {
            val jobs = mutableListOf<Job>()

            for (entry in modpack.entrySet) {
                if (entry.side == Side.SERVER) continue

                jobs += launch(context = coroutineContext + pool) {
                    val folder = minecraftDir.resolve(entry.serialFile).absoluteFile.parentFile

                    val matchedFeatureList = modpack.features.filter { it.entries.contains(entry.id) }
                    val selected = !matchedFeatureList.isEmpty() && matchedFeatureList.any {
                        features[it.feature.name] ?: false
                    }

                    val provider = Providers[entry.provider]
                    val targetFolder = minecraftDir.resolve(folder)
                    val (_, file) = provider.download(entry, targetFolder, cacheDir)
                    if (!selected) {
                        file.renameTo(file.parentFile.resolve(file.name + ".disabled"))
                    }
                }
            }

            delay(10)
            CursePack.logger.info("waiting for jobs to finish")
            jobs.joinAll()
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

        targetDir.mkdirs()
        val instanceZip = targetDir.resolve(modpack.id + ".zip")

        instanceZip.delete()
        packToZip(instanceDir.toPath(), instanceZip.toPath())
        logger.info("created mmc pack $instanceZip")
    }
}