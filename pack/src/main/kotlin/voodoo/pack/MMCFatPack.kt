package voodoo.pack

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.internal.BooleanSerializer
import kotlinx.serialization.internal.HashMapSerializer
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.map
import kotlinx.serialization.serializer
import voodoo.data.Side
import voodoo.data.lock.LockPack
import voodoo.forge.ForgeUtil
import voodoo.mmc.MMCUtil
import voodoo.provider.Providers
import voodoo.util.Downloader
import voodoo.util.blankOr
import voodoo.util.packToZip
import voodoo.util.pool

object MMCFatPack : AbstractPack() {
    override val label = "MultiMC Packer (frozen pack)"

    override suspend fun pack(
        modpack: LockPack,
        target: String?,
        clean: Boolean
    ) {
        val targetDir = modpack.rootDir.resolve(target ?: ".multimc")
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
            featureJson.writeText(json.stringify((StringSerializer to BooleanSerializer).map, features))
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