package voodoo.tester

import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.serialization.internal.BooleanSerializer
import kotlinx.serialization.internal.HashMapSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.serializer
import voodoo.data.Side
import voodoo.data.lock.LockPack
import voodoo.mmc.MMCUtil
import voodoo.provider.Providers
import voodoo.util.blankOr
import voodoo.util.downloader
import voodoo.util.pool
import java.io.File

/**
 * Created by nikky on 06/05/18.
 * @author Nikky
 */

object MultiMCTester : AbstractTester() {
    override val label = "MultiMC Tester"

    override suspend fun execute(modpack: LockPack, clean: Boolean) {
        val folder = "voodoo_test_${modpack.id}"
        val title = "${modpack.title.blankOr ?: modpack.id} Test Instance"

        val cacheDir = directories.cacheHome
        val mmcConfigDir = File("multimc")
        val multimcDir = MMCUtil.findDir()
        val instanceDir = multimcDir.resolve("instances").resolve(folder)

        if (clean) {
            logger.info("cleaning old instance dir")
            instanceDir.deleteRecursively()
        }

        instanceDir.mkdirs()

        val minecraftDir = MMCUtil.installEmptyPack(title, folder, icon = modpack.iconFile, mcVersion = modpack.mcVersion, forgeBuild = modpack.forge)

        minecraftDir.mkdirs()
        val modsDir = minecraftDir.resolve("mods")
        modsDir.deleteRecursively()

        downloader.logger.info("copying files into minecraft dir")
        val minecraftSrcDir = modpack.sourceFolder
        if (minecraftSrcDir.exists()) {
            minecraftSrcDir.copyRecursively(minecraftDir, overwrite = true)
        }

        for (file in minecraftDir.walkTopDown()) {
            when {
                file.name.endsWith(".lock.hjson") -> file.delete()
                file.name.endsWith(".entry.hjson") -> file.delete()
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
        val featureSerializer = HashMapSerializer(String.serializer(), BooleanSerializer)

        // read user input
        val featureJson = instanceDir.resolve("voodoo.features.json")
        val previousSelection = if (featureJson.exists()) {
            json.parse(featureSerializer, featureJson.readText())
        } else {
            mapOf<String, Boolean>()
        }
        val (features, reinstall) = MMCUtil.selectFeatures(modpack.features.map { it.properties }, previousSelection,
                modpack.title.blankOr
                        ?: modpack.id, modpack.version, forceDisplay = false, updating = featureJson.exists())
        logger.debug("result: features: $features")
        if (!features.isEmpty()) {
            featureJson.createNewFile()
            featureJson.writeText(json.stringify(featureSerializer, features))
        }
        if (reinstall) {
            minecraftDir.deleteRecursively()
        }

        runBlocking(context = pool) {
            modpack.entrySet.forEach { entry ->
                if (entry.side == Side.SERVER) return@forEach
                launch {
                    val folder = minecraftDir.resolve(entry.file).absoluteFile.parentFile

                    val matchedFeatureList = modpack.features.filter { it.entries.contains(entry.id) }
                    if (!matchedFeatureList.isEmpty()) {
                        val download = matchedFeatureList.any { features[it.properties.name] ?: false }
                        if (!download) {
                            MMCUtil.logger.info("${matchedFeatureList.map { it.properties.name }} is disabled, skipping download")
                            return@launch
                        }
                    }
                    val provider = Providers[entry.provider]
                    val targetFolder = minecraftDir.resolve(folder)
                    val (url, file) = provider.download(entry, targetFolder, cacheDir)
                }

            }
        }

        logger.info("clearing serverside files")
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

//        MMCUtil.startInstance(title)
        MMCUtil.startInstance(folder)
    }


}