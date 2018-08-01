package voodoo.pack

import blue.endless.jankson.Jankson
import voodoo.data.lock.LockPack
import voodoo.mmc.MMCUtil
import voodoo.util.jenkins.DownloadVoodoo
import voodoo.util.packToZip
import java.io.File
import kotlin.system.exitProcess

object MMCPack : AbstractPack() {
    override val label = "MultiMC Packer"

    override suspend fun download(rootFolder: File, modpack: LockPack, target: String?, clean: Boolean, jankson: Jankson) {
        val targetDir = File(target ?: ".multimc")
        val definitionsDir = File("multimc").apply { mkdirs() }
        val cacheDir = directories.cacheHome.resolve("mmc")
        val instanceDir = cacheDir.resolve(modpack.id)
        instanceDir.deleteRecursively()

        val iconFile = definitionsDir.resolve("${modpack.id}.icon.png")
        val preLaunchCommand = "\"\$INST_JAVA\" -jar \"\$INST_DIR/mmc-installer.jar\" --id \"\$INST_ID\" --inst \"\$INST_DIR\" --mc \"\$INST_MC_DIR\""
        val minecraftDir = MMCUtil.installEmptyPack(modpack.title, modpack.id, icon = iconFile, instanceDir = instanceDir, preLaunchCommand = preLaunchCommand)

        logger.info("tmp dir: $instanceDir")

        val urlFile = definitionsDir.resolve("${modpack.id}.url.txt")
        if (!urlFile.exists()) {
            logger.error("no file '${urlFile.absolutePath}' found")
            exitProcess(3)
        }
        urlFile.copyTo(instanceDir.resolve("voodoo.url.txt"))

        val multimcInstaller = instanceDir.resolve("mmc-installer.jar")
        val installer = DownloadVoodoo.downloadVoodoo(component = "multimc-installer", bootstrap = true,  fat = false, binariesDir = directories.cacheHome)
        installer.copyTo(multimcInstaller)

        val packignore = instanceDir.resolve(".packignore")
        packignore.writeText(
                """.minecraft
                  |mmc-pack.json
                """.trimMargin()
        )

        targetDir.mkdirs()
        val instanceZip = targetDir.resolve(modpack.id + ".zip")

        instanceZip.delete()
        packToZip(instanceDir.toPath(), instanceZip.toPath())
        logger.info("created mmc pack $instanceZip")
    }
}