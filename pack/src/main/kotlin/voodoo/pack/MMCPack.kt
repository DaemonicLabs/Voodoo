package voodoo.pack

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import voodoo.data.lock.LockPack
import voodoo.mmc.MMCUtil
import voodoo.util.jenkins.JenkinsServer
import voodoo.util.packToZip
import java.io.File
import kotlin.system.exitProcess

object MMCPack : AbstractPack() {
    override val label = "MultiMC Packer"

    override fun download(modpack: LockPack, target: String?, clean: Boolean) {
        val targetDir = File(target ?: ".multimc")
        val definitionsDir = File("multimc").apply { mkdirs() }
        val cacheDir = directories.cacheHome.resolve("mmc")
        val instanceDir = cacheDir.resolve(modpack.name)
        instanceDir.deleteRecursively()

        val iconFile = definitionsDir.resolve("${modpack.name}.icon.png")
        val preLaunchCommand = "\"\$INST_JAVA\" -jar \"\$INST_DIR/mmc-installer.jar\" --id \"\$INST_ID\" --inst \"\$INST_DIR\" --mc \"\$INST_MC_DIR\""
        val minecraftDir = MMCUtil.installEmptyPack(modpack.title, modpack.name, icon = iconFile, instanceDir = instanceDir, preLaunchCommand = preLaunchCommand)

        logger.info("tmp dir: $instanceDir")

        val urlFile = definitionsDir.resolve("${modpack.name}.url.txt")
        if (!urlFile.exists()) {
            logger.error("no file '${urlFile.absolutePath}' found")
            exitProcess(3)
        }
        urlFile.copyTo(instanceDir.resolve("voodoo.url.txt"))

        val serverInstaller = instanceDir.resolve("mmc-installer.jar")
        val installer = downloadInstaller()
        installer.copyTo(serverInstaller)

        val packignore = instanceDir.resolve(".packignore")
        packignore.writeText(
                """.minecraft
                  |mmc-pack.json
                """.trimMargin()
        )

        targetDir.mkdirs()
        val instanceZip = targetDir.resolve(modpack.name + ".zip")

        instanceZip.delete()
        packToZip(instanceDir.toPath(), instanceZip.toPath())
        logger.info("created mmc pack $instanceZip")
    }


    const val FILE_REGEX = "multimc-installer-[^-]*(?!-fat)\\.jar"
    const val JENKINS_URL = "https://ci.elytradev.com"
    const val JENKINS_JOB = "elytra/Voodoo/master"
    const val MODULE_NAME = "multimc-installer"

    private fun downloadInstaller(): File {
        val userAgent = "voodoo-pack/$VERSION"
        val binariesDir = directories.cacheHome

        val server = JenkinsServer(JENKINS_URL)
        val job = server.getJob(JENKINS_JOB, userAgent)!!
        val build = job.lastSuccessfulBuild?.details(userAgent)!!
        val buildNumber = build.number
        logger.info("lastSuccessfulBuild: $buildNumber")
        logger.debug("looking for $FILE_REGEX")
        val re = Regex(FILE_REGEX)
        val artifact = build.artifacts.find {
            logger.debug(it.fileName)
            re.matches(it.fileName)
        }
        if (artifact == null) {
            logger.error("did not find {} in {}", FILE_REGEX, build.artifacts)
            throw Exception()
        }
        val url = build.url + "artifact/" + artifact.relativePath
        val tmpFile = File(binariesDir, "$MODULE_NAME-$buildNumber.tmp")
        val targetFile = File(binariesDir, "$MODULE_NAME-$buildNumber.jar")
        if (!targetFile.exists()) {
            val (_, _, result) = url.httpGet()
                    .header("User-Agent" to userAgent)
                    .response()
            when (result) {
                is Result.Success -> {
                    tmpFile.writeBytes(result.value)
                    tmpFile.renameTo(targetFile)
                }
                is Result.Failure -> {
                    logger.error(result.error.toString())
                    throw Exception("unable to download jarfile from $url")
                }
            }
        }
        return targetFile
    }
}