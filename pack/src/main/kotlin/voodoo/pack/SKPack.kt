package voodoo.pack

import mu.KotlinLogging
import voodoo.data.lock.LockPack
import voodoo.forge.Forge
import voodoo.pack.sk.SKFeature
import voodoo.pack.sk.SKLocation
import voodoo.pack.sk.SKModpack
import voodoo.pack.sk.SKWorkspace
import voodoo.provider.Provider
import voodoo.util.download
import voodoo.util.readJson
import voodoo.util.writeJson
import java.io.File

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 * @version 1.0
 */

object SKPack : AbstractPack() {
    private val logger = KotlinLogging.logger {}

    override val label = "SK Packer"

    override fun download(modpack: LockPack, target: String?, clean: Boolean) {
        val cacheDir = directories.cacheHome
        val targetDir = File(target ?: "workspace")
        val modpackDir = targetDir.resolve(modpack.name)

        val srcFolder = modpackDir.resolve("src")
        if (clean) {
            logger.info("cleaning modpack directory $srcFolder")
            srcFolder.deleteRecursively()
        }
        if (!srcFolder.exists()) {
            logger.info("copying files into src")
            val mcDir = File(modpack.minecraftDir)
            if(mcDir.exists()) {
                mcDir.copyRecursively(srcFolder, overwrite = true)
            } else {
                logger.warn("directory $mcDir does not exist")
            }
        }

        val loadersFolder = modpackDir.resolve("loaders")
        logger.info("cleaning loaders $loadersFolder")
        loadersFolder.deleteRecursively()

        // download forge
        val (forgeUrl, forgeFileName, forgeLongVersion, forgeVersion) = Forge.getForgeUrl(modpack.forge.toString(), modpack.mcVersion)
        val forgeFile = loadersFolder.resolve(forgeFileName)
        forgeFile.download(forgeUrl, cacheDir.resolve("FORGE").resolve(forgeVersion))

//        val modsFolder = srcFolder.resolve("mods")
//        logger.info("cleaning mods $modsFolder")
//        modsFolder.deleteRecursively()

        val targetFiles = mutableMapOf<String, String>()
        // download entries
//        srcFolder.resolve("mods").deleteRecursively()
        for (entry in modpack.entries) {
            val provider = Provider.valueOf(entry.provider).base
            val (url, file) = provider.download(entry, modpack, srcFolder.resolve(entry.folder), cacheDir)
            if (url != null && entry.useUrlTxt) {
                val urlTxtFile = file.parentFile.resolve(file.name + ".url.txt")
                urlTxtFile.writeText(url)
            }
            targetFiles[entry.name] = file.relativeTo(srcFolder).path
        }

        // write features
        val features = mutableListOf<SKFeature>()
        for (feature in modpack.features) {
            for (name in feature.entries) {
                feature.files.include += targetFiles[name]!!
                logger.info("includes = ${feature.files.include}")
            }

            features += SKFeature(
                    properties = feature.properties,
                    files = feature.files
            )
            logger.info("processed feature $feature")

        }


        val skmodpack = SKModpack(
                name = modpack.name,
                title = modpack.title,
                gameVersion = modpack.mcVersion,
                userFiles = modpack.userFiles,
                launch = modpack.launch,
                features = features
        )
        val modpackPath = modpackDir.resolve("modpack.json")
        modpackPath.writeJson(skmodpack)

        // add to workspace.json
        logger.info("adding {} to workpace.json", modpack.name)
        val worspaceFolder = targetDir.resolve(".modpacks")
        worspaceFolder.mkdirs()
        val workspacePath = worspaceFolder.resolve("workspace.json")
        val workspace = if (workspacePath.exists()) {
            workspacePath.readJson()
        } else {
            SKWorkspace()
        }
        workspace.packs += SKLocation(modpack.name)

        workspacePath.writeJson(workspace)
    }

}