package voodoo.pack

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import voodoo.data.lock.LockPack
import voodoo.util.DownloadVoodoo
import voodoo.util.jenkins.JenkinsServer
import voodoo.util.writeJson
import java.io.File

/**
 * Created by nikky on 06/05/18.
 * @author Nikky
 */

object ServerPack : AbstractPack() {
    override val label = "Server SKPack"

    override fun download(modpack: LockPack, target: String?, clean: Boolean) {
        val targetDir = File(target ?: ".server")
        val modpackDir = targetDir.resolve(modpack.name)

        if (clean) {
            logger.info("cleaning server directory $modpackDir")
            modpackDir.deleteRecursively()
        }

        modpackDir.mkdirs()

        val localDir = File(modpack.localDir)
        logger.info("local: $localDir")
        if(localDir.exists()) {
            val targetLocalDir = modpackDir.resolve("local")
            modpack.localDir = targetLocalDir.name

            if(targetLocalDir.exists()) targetLocalDir.deleteRecursively()
            targetLocalDir.mkdirs()

            localDir.copyRecursively(targetLocalDir, true)
        }

        val minecraftDir = File(modpack.minecraftDir)
        logger.info("mcDir: $minecraftDir")
        if(minecraftDir.exists()) {
            val targetMinecraftDir = modpackDir.resolve("minecraft")
            modpack.minecraftDir = targetMinecraftDir.name

            if(targetMinecraftDir.exists()) targetMinecraftDir.deleteRecursively()
            targetMinecraftDir.mkdirs()

            minecraftDir.copyRecursively(targetMinecraftDir, true)
        }

        val packFile = modpackDir.resolve("pack.lock.json")
        packFile.writeJson(modpack)


        logger.info("packaging installer jar")
        val installer = DownloadVoodoo.downloadVoodoo(component = "server-installer", bootstrap = false,  fat = false, binariesDir = directories.cacheHome)

        val serverInstaller = modpackDir.resolve("server-installer.jar")
        installer.copyTo(serverInstaller)

        logger.info("server package ready: ${modpackDir.absolutePath}")
    }
}