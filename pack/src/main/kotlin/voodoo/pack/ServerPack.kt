package voodoo.pack

import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonObject
import voodoo.data.lock.LockPack
import voodoo.util.DownloadVoodoo
import java.io.File

/**
 * Created by nikky on 06/05/18.
 * @author Nikky
 */

object ServerPack : AbstractPack() {
    override val label = "Server SKPack"

    override fun download(rootFolder: File, modpack: LockPack, target: String?, clean: Boolean, jankson: Jankson) {
        val targetDir = File(target ?: ".server")
        val modpackDir = targetDir.resolve(modpack.name)

        if (clean) {
            logger.info("cleaning server directory $modpackDir")
            modpackDir.deleteRecursively()
        }

        modpackDir.mkdirs()

        val localDir = rootFolder.resolve(modpack.localDir)
        logger.info("local: $localDir")
        if (localDir.exists()) {
            val targetLocalDir = modpackDir.resolve("local")
            modpack.localDir = targetLocalDir.name

            if (targetLocalDir.exists()) targetLocalDir.deleteRecursively()
            targetLocalDir.mkdirs()

            localDir.copyRecursively(targetLocalDir, true)
        }

        val sourceDir = rootFolder.resolve(modpack.sourceDir)
        logger.info("mcDir: $sourceDir")
        if (sourceDir.exists()) {
            val targetSourceDir = modpackDir.resolve("src")
            modpack.sourceDir = targetSourceDir.name

            if (targetSourceDir.exists()) targetSourceDir.deleteRecursively()
            targetSourceDir.mkdirs()

            sourceDir.copyRecursively(targetSourceDir, true)
            targetSourceDir.walkBottomUp().forEach { file ->
                if (file.name.endsWith(".entry.hjson"))
                    file.delete()
                if (file.isDirectory && file.listFiles().isEmpty()) {
                    file.delete()
                }
                when {
                    file.name == "_CLIENT" -> file.deleteRecursively()
                    file.name == "_SERVER" -> {
                        file.copyRecursively(file.absoluteFile.parentFile, overwrite = true)
                        file.deleteRecursively()
                    }
                }
            }
        }

        val packFile = modpackDir.resolve("pack.lock.json")

        val defaultJson = JsonObject() //TODO: get default pack ?
        val lockJson = jankson.toJson(modpack) as JsonObject
        val delta = lockJson.getDelta(defaultJson)
        packFile.writeText(delta.toJson(true, true).replace("\t", "  "))


        logger.info("packaging installer jar")
        val installer = DownloadVoodoo.downloadVoodoo(component = "server-installer-fat", bootstrap = false, fat = false, binariesDir = directories.cacheHome)

        val serverInstaller = modpackDir.resolve("server-installer.jar")
        installer.copyTo(serverInstaller)

        logger.info("server package ready: ${modpackDir.absolutePath}")
    }
}