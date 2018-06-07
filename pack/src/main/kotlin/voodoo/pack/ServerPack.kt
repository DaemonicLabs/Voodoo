package voodoo.pack

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import voodoo.data.lock.LockPack
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
        val installer = downloadInstaller()

        val serverInstaller = modpackDir.resolve("server-installer.jar")
        installer.copyTo(serverInstaller)

        logger.info("server package ready: ${modpackDir.absolutePath}")
    }

    const val FILE_REGEX = "server-installer-[^-]*(?!-fat)\\.jar"
    const val JENKINS_URL = "https://ci.elytradev.com"
    const val JENKINS_JOB = "elytra/Voodoo/master"
    const val MODULE_NAME = "server-installer"

    //TODO: generalize for all jars and allow to pick local compiled jar
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
        if(artifact == null) {
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