package voodoo.tester

import voodoo.data.Side
import voodoo.data.lock.LockPack
import voodoo.mmc.MMCUtil
import voodoo.provider.Provider
import voodoo.util.blankOr
import voodoo.util.downloader
import voodoo.util.readJson
import voodoo.util.writeJson
import java.io.File

/**
 * Created by nikky on 06/05/18.
 * @author Nikky
 */

object MultiMCTester : AbstractTester() {
    override val label = "MultiMC Tester"

    override suspend fun execute(modpack: LockPack, clean: Boolean) {
        val folder = "voodoo_test_${modpack.name}"
        val title = "${modpack.title.blankOr ?: modpack.name} Voodoo Test"

        val cacheDir = directories.cacheHome
        val multimcDir = MMCUtil.findDir()
        val instanceDir = multimcDir.resolve("instances").resolve(folder)

        if(clean) {
            logger.info("cleaning old instance dir")
            instanceDir.deleteRecursively()
        }

        instanceDir.mkdirs()

        val iconFile = File("multimc").resolve("${modpack.name}.icon.png")
        val minecraftDir = MMCUtil.installEmptyPack(title, folder, icon = iconFile, mcVersion = modpack.mcVersion, forgeBuild = modpack.forge)

        minecraftDir.mkdirs()
        val modsDir = minecraftDir.resolve("mods")
        modsDir.deleteRecursively()

        downloader.logger.info("copying files into minecraft dir")
        val minecraftSrcDir = File(modpack.sourceDir)
        if (minecraftSrcDir.exists()) {
            minecraftSrcDir.copyRecursively(minecraftDir, overwrite = true)
        }

        for(file in minecraftDir.walkTopDown()) {
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

        // read user input
        val featureJson = instanceDir.resolve("voodoo.features.json")
        val defaults = if (featureJson.exists()) {
            featureJson.readJson()
        } else {
            mapOf<String, Boolean>()
        }
        val features = MMCUtil.selectFeatures(modpack.features.map { it.properties }, defaults,
                modpack.title.blankOr ?: modpack.name, modpack.version)
        if (!features.isEmpty()) {
            featureJson.createNewFile()
            featureJson.writeJson(features)
        }

        for ( (name, pair) in modpack.entriesMapping) {
            val (entry, entryFile) = pair
            val folder = minecraftDir.resolve(entryFile).absoluteFile.parentFile
            if (entry.side == Side.SERVER) continue
            val matchedFeatureList = modpack.features.filter { it.entries.contains(entry.name) }
            if (!matchedFeatureList.isEmpty()) {
                val download = matchedFeatureList.any { features[it.properties.name] ?: false }
                if (!download) {
                    MMCUtil.logger.info("${matchedFeatureList.map { it.properties.name }} is disabled, skipping download")
                    continue
                }
            }
            val provider = Provider.valueOf(entry.provider).base
            val targetFolder = minecraftDir.resolve(folder)
            val (url, file) = provider.download(entry, targetFolder, cacheDir)
        }

        MMCUtil.startInstance(folder)
    }


}