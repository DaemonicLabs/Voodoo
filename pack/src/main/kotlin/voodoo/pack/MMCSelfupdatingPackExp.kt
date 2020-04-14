package voodoo.pack

import com.eyeem.watchadoin.Stopwatch
import voodoo.data.lock.LockPack
import voodoo.mmc.MMCUtil
import voodoo.util.blankOr
import voodoo.util.maven.MavenUtil
import voodoo.util.packToZip
import voodoo.util.unixPath
import java.io.File
import java.net.URI
import kotlin.system.exitProcess

object MMCSelfupdatingPackExp : AbstractPack("mmc-experimental") {
    override val label = "MultiMC Pack"

    override fun File.getOutputFolder(id: String): File = resolve("multimc-experimental")

    override suspend fun pack(
        stopwatch: Stopwatch,
        modpack: LockPack,
        output: File,
        uploadBaseDir: File,
        clean: Boolean
    ) = stopwatch {
        val cacheDir = directories.cacheHome
        val instanceDir = cacheDir.resolve("MMC").resolve(modpack.id)
        instanceDir.deleteRecursively()

        val installerFilename = "mmc-installer.jar"
        val preLaunchCommand =
            "\"\$INST_JAVA\" -jar \"\$INST_DIR/$installerFilename\" --id \"\$INST_ID\" --inst \"\$INST_DIR\" --mc \"\$INST_MC_DIR\""
        val minecraftDir = MMCUtil.installEmptyPack(
            modpack.title.blankOr,
            modpack.id,
            icon = modpack.iconFile,
            modloader = modpack.modloader,
            extraCfg = modpack.packOptions.multimcOptions.instanceCfg,
            instanceDir = instanceDir,
            preLaunchCommand = preLaunchCommand
        )

        logger.info("created pack in $minecraftDir")
        logger.info("tmp dir: $instanceDir")

        val selfupdateUrl = modpack.packOptions.multimcOptions.selfupdateUrl
            ?: run {
                modpack.packOptions.baseUrl?.let { baseUrl ->
                    val skOutput = with(SKPack) { uploadBaseDir.getOutputFolder(modpack.id) }
                    val skPackFile = skOutput.resolve("${modpack.id}.json")
                    val relativePath = skPackFile.relativeTo(uploadBaseDir).unixPath
                    URI(baseUrl).resolve(relativePath).toASCIIString()
                }
            }
        if (selfupdateUrl == null) {
            logger.error("selfupdateUrl in multimc options is not set")
            exitProcess(3)
        }
        val urlFile = instanceDir.resolve("voodoo.url.txt")
        urlFile.writeText(selfupdateUrl)

        val multimcInstaller = instanceDir.resolve(installerFilename)
        val installer = MavenUtil.downloadArtifact(
            stopwatch = "downloadArtifact multimc installer bootstrap".watch,
            mavenUrl = PackConstants.MAVEN_URL,
            group = PackConstants.MAVEN_GROUP,
            artifactId = "bootstrap",
            version = PackConstants.FULL_VERSION,
            classifier = "multimc-installer",
            outputFile =  multimcInstaller,
            outputDir = directories.cacheHome
        )
//        installer.copyTo(multimcInstaller)

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