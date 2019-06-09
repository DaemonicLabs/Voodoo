package voodoo.pack

import voodoo.data.lock.LockPack
import voodoo.mmc.MMCUtil
import voodoo.util.blankOr
import voodoo.util.jenkins.downloadVoodoo
import voodoo.util.packToZip
import voodoo.util.unixPath
import java.io.File
import java.net.URI
import kotlin.system.exitProcess

object MMCPack : AbstractPack() {
    override val label = "MultiMC Pack"

    override fun File.getOutputFolder(id: String): File = resolve("multimc-sk")

    override suspend fun pack(
        modpack: LockPack,
        output: File,
        uploadBaseDir: File,
        clean: Boolean
    ) {
        val cacheDir = directories.cacheHome
        val instanceDir = cacheDir.resolve("MMC").resolve(modpack.id)
        instanceDir.deleteRecursively()

        val preLaunchCommand =
            "\"\$INST_JAVA\" -jar \"\$INST_DIR/mmc-installer.jar\" --categoryId \"\$INST_ID\" --inst \"\$INST_DIR\" --mc \"\$INST_MC_DIR\""
        val minecraftDir = MMCUtil.installEmptyPack(
            modpack.title.blankOr,
            modpack.id,
            icon = modpack.iconFile,
            instanceDir = instanceDir,
            preLaunchCommand = preLaunchCommand
        )

        logger.info("created pack in $minecraftDir")
        logger.info("tmp dir: $instanceDir")

        val skPackUrl = modpack.packOptions.multimcOptions.skPackUrl
            ?: run {
                modpack.packOptions.baseUrl?.let { baseUrl ->
                    val skOutput = with(SKPack) { uploadBaseDir.getOutputFolder(modpack.id) }
                    val skPackFile = skOutput.resolve("${modpack.id}.json")
                    val relativePath = skPackFile.relativeTo(uploadBaseDir).unixPath
                    URI(baseUrl).resolve(relativePath).toASCIIString()
                }
            }
        if (skPackUrl == null) {
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

        output.mkdirs()
        val instanceZip = output.resolve(modpack.id + ".zip")

        instanceZip.delete()
        packToZip(instanceDir.toPath(), instanceZip.toPath())
        logger.info("created mmc pack $instanceZip")
    }
}