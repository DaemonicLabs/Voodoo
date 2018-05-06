package voodoo.pack

import voodoo.data.Side
import voodoo.data.lock.LockPack
import voodoo.forge.Forge
import voodoo.provider.Provider
import voodoo.util.download
import voodoo.util.downloader.logger
import java.io.File

/**
 * Created by nikky on 06/05/18.
 * @author Nikky
 * @version 1.0
 */

object ServerPack : AbstractPack() {
    override val label = "Server Pack"

    override fun download(modpack: LockPack, target: String?, clean: Boolean) {
        val cacheDir = SKPack.directories.cacheHome
        val targetDir = File(target ?: "server")
        val modpackDir = targetDir.resolve(modpack.name)

        val srcFolder = modpackDir.resolve("src")
        if (clean) {
            logger.info("cleaning modpack directory $modpackDir")
            modpackDir.deleteRecursively()
        }
        if (!srcFolder.exists()) {
            logger.info("copying files into src")
            val mcDir = File(modpack.minecraftDir)
            if (mcDir.exists()) {
                mcDir.copyRecursively(srcFolder, overwrite = true) //TODOO filter _CLIENT and strip _SERVER from paths
            } else {
                logger.warn("minecraft directory $mcDir does not exist")
            }
        }

        for(file in srcFolder.walkTopDown()) {
            when {
                file.name == "_CLIENT" -> file.deleteRecursively()
                file.name == "_SERVER" -> file.renameTo(file.parentFile)
            }
        }

        // download forge
        val (forgeUrl, forgeFileName, forgeLongVersion, forgeVersion) = Forge.getForgeUrl(modpack.forge.toString(), modpack.mcVersion)
        val forgeFile = modpackDir.resolve(forgeFileName)
        forgeFile.download(forgeUrl, cacheDir.resolve("FORGE").resolve(forgeVersion))


        // download entries
        val targetFiles = mutableMapOf<String, File>()
        for (entry in modpack.entries) {
            if(entry.side == Side.CLIENT) continue
            val provider = Provider.valueOf(entry.provider).base
            val targetFolder = srcFolder.resolve(entry.folder)
            val (url, file) = provider.download(entry, modpack, targetFolder, cacheDir)
            if (url != null && entry.useUrlTxt) {
                val urlTxtFile = file.parentFile.resolve(file.name + ".url.txt")
                urlTxtFile.writeText(url)
            }
            targetFiles[entry.name] = file.relativeTo(srcFolder)
        }

    }


}