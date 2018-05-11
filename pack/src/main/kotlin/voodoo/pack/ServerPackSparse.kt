package voodoo.pack

import voodoo.data.lock.LockPack
import voodoo.util.downloader.logger
import voodoo.util.writeJson
import java.io.File

/**
 * Created by nikky on 06/05/18.
 * @author Nikky
 * @version 1.0
 */

object ServerPackSparse : AbstractPack() {
    override val label = "Server Pack"

    override fun download(modpack: LockPack, target: String?, clean: Boolean) {
        val targetDir = File(target ?: "server")
        val modpackDir = targetDir.resolve(modpack.name)

        if (clean) {
            logger.info("cleaning modpack directory $modpackDir")
            modpackDir.deleteRecursively()
        }

        val localDir = File(modpack.localDir)
        if(localDir.exists()) {
            val targetLocalDir = modpackDir.resolve("local")
            modpack.localDir = targetLocalDir.name

            if(targetLocalDir.exists()) targetLocalDir.deleteRecursively()
            targetLocalDir.mkdirs()

            localDir.copyRecursively(targetLocalDir, true)
        }

        val minecraftDir = File(modpack.minecraftDir)
        if(localDir.exists()) {
            val targetMinecraftDir = modpackDir.resolve("minecraft")
            modpack.minecraftDir = targetMinecraftDir.name

            if(targetMinecraftDir.exists()) targetMinecraftDir.deleteRecursively()
            targetMinecraftDir.mkdirs()

            minecraftDir.copyRecursively(targetMinecraftDir, true)
        }

        val packFile = modpackDir.resolve("pack.lock.json")
        packFile.writeJson(modpack)
    }


}