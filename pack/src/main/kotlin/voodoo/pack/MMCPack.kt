package voodoo.pack

import voodoo.data.lock.LockPack
import voodoo.mmc.MMCUtil
import voodoo.util.jenkins.downloadVoodoo
import voodoo.util.packToZip
import kotlin.system.exitProcess

object MMCPack : AbstractPack() {
    override val label = "MultiMC Packer"

    override suspend fun pack(
        modpack: LockPack,
        target: String?,
        clean: Boolean
    ) {
        val targetDir = modpack.rootDir.resolve(target ?: ".multimcOptions")
        val cacheDir = directories.cacheHome
        val instanceDir = cacheDir.resolve("MMC").resolve(modpack.id)
        instanceDir.deleteRecursively()

        val preLaunchCommand =
            "\"\$INST_JAVA\" -jar \"\$INST_DIR/mmc-installer.jar\" --id \"\$INST_ID\" --inst \"\$INST_DIR\" --mc \"\$INST_MC_DIR\""
        val minecraftDir = MMCUtil.installEmptyPack(
            modpack.title,
            modpack.id,
            icon = modpack.icon,
            instanceDir = instanceDir,
            preLaunchCommand = preLaunchCommand
        )

        logger.info("created pack in $minecraftDir")
        logger.info("tmp dir: $instanceDir")

        val skPackUrl = modpack.packOptions.multimcOptions.skPackUrl
        if(skPackUrl == null) {
            logger.error("skPackUrl in multimc options is not set")
            exitProcess(3)
        }
        val urlFile = instanceDir.resolve("voodoo.url.txt")
        urlFile.writeText(skPackUrl)

        val multimcInstaller = instanceDir.resolve("mmc-installer.jar")
        val installer =
            downloadVoodoo(component = "multimc-installer", bootstrap = true, binariesDir = directories.cacheHome)
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