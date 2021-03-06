package voodoo.pack

import com.eyeem.watchadoin.Stopwatch
import mu.KotlinLogging
import voodoo.data.lock.LockPack
import voodoo.mmc.MMCUtil
import voodoo.util.Directories
import voodoo.util.blankOr
import voodoo.util.maven.MavenUtil
import voodoo.util.packToZip
import java.io.File
import java.net.URI
import kotlin.system.exitProcess

object MMCSelfupdatingPackVoodoo : AbstractPack("mmc-voodoo") {
    private val logger = KotlinLogging.logger {}
    override val label = "MultiMC Pack"

    override fun File.getOutputFolder(id: String, version: String): File = resolve("multimc-voodoo")

    override suspend fun pack(
        stopwatch: Stopwatch,
        modpack: LockPack,
        config: PackConfig,
        output: File,
        uploadBaseDir: File,
        clean: Boolean,
        versionAlias: String?
    ) = stopwatch {
        val directories = Directories.get()

        val cacheDir = directories.cacheHome
        val zipRootDir = cacheDir.resolve("MULTIMC").resolve(modpack.id).resolve(versionAlias ?: modpack.version)
        val instanceDir = zipRootDir.resolve(modpack.id)
        zipRootDir.deleteRecursively()

        val installer = "downloadArtifact multimc installer".watch {
            MavenUtil.downloadArtifact(
                mavenUrl = GeneratedConstants.MAVEN_URL,
                group = GeneratedConstants.MAVEN_GROUP,
                artifactId = "multimc-installer",
                version = GeneratedConstants.FULL_VERSION,
                classifier = GeneratedConstants.MAVEN_SHADOW_CLASSIFIER,
                outputFile = instanceDir.resolve(".voodoo").resolve("multimc-installer.jar"),
                outputDir = instanceDir.resolve(".voodoo")
            )
        }
        val installerFilename = installer.toRelativeString(instanceDir).replace('\\', '/')
        val preLaunchCommand =
            "\"\$INST_JAVA\" -jar \"\$INST_DIR/$installerFilename\" --id \"\$INST_ID\" --inst \"\$INST_DIR\" --mc \"\$INST_MC_DIR\" --phase PRE"
        val postExitCommand =
            "\"\$INST_JAVA\" -jar \"\$INST_DIR/.voodoo/post.jar\" --id \"\$INST_ID\" --inst \"\$INST_DIR\" --mc \"\$INST_MC_DIR\" --phase POST"
        val minecraftDir = MMCUtil.installEmptyPack(
            modpack.title.blankOr,
            modpack.id,
            icon = modpack.iconFile,
            modloader = modpack.modloader,
            extraCfg = modpack.packOptions.multimcOptions.instanceCfg,
            instanceDir = instanceDir,
            preLaunchCommand = preLaunchCommand,
            postExitCommand = postExitCommand
        )

        logger.info { "created pack in $minecraftDir" }
        logger.info { "tmp dir: $instanceDir" }
        val selfupdateUrl = modpack.packOptions.uploadUrl?.let { uploadUrl ->
            val relativeSelfupdateUrl = (modpack.packOptions.multimcOptions.relativeSelfupdateUrl ?: "${modpack.id}.json")
            URI(uploadUrl).resolve(relativeSelfupdateUrl).toASCIIString()
        }
        if (selfupdateUrl == null) {
            logger.error { "selfupdateUrl in multimc options is not set" }
            exitProcess(3)
        }
        val urlFile = instanceDir.resolve("voodoo.url.txt")
        urlFile.writeText(selfupdateUrl)

        val packignore = instanceDir.resolve(".packignore")
        packignore.writeText(
            """.minecraft
                  |mmc-pack.json
                """.trimMargin()
        )

        output.mkdirs()
        val instanceZipVersioned = output.resolve("${modpack.id}-${versionAlias ?: modpack.version}.zip")
        val instanceZip = output.resolve("${modpack.id}.zip")

        instanceZip.delete()
        packToZip(zipRootDir, instanceZip)

        instanceDir.resolve("voodoo.version.txt").writeText(versionAlias ?: modpack.version)

        instanceZipVersioned.delete()
        packToZip(zipRootDir, instanceZipVersioned)
        logger.info { "created mmc pack $instanceZip" }
    }
}