package voodoo.pack

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import voodoo.data.lock.LockPack
import voodoo.forge.Forge
import voodoo.mmc.MMCUtil
import voodoo.mmc.data.MultiMCPack
import voodoo.mmc.data.PackComponent
import voodoo.util.jenkins.JenkinsServer
import voodoo.util.packToZip
import voodoo.util.readJson
import voodoo.util.writeJson
import java.io.File
import kotlin.system.exitProcess

object MMCPack : AbstractPack() {
    override val label = "MultiMC Packer"

    override fun download(modpack: LockPack, target: String?, clean: Boolean) {
        val targetDir = File(target ?: ".multimc")
        val cacheDir = directories.cacheHome.resolve("mmc")
        val wrapperDir = cacheDir.resolve(modpack.name).apply { deleteRecursively() }
        val instanceDir = wrapperDir.resolve(modpack.name).apply { mkdirs() }

        val urlFile = File("${modpack.name}.url.txt")
        if(!urlFile.exists()) {
            logger.error("no file '$urlFile' found")
            exitProcess(3)
        }
        urlFile.copyTo(instanceDir.resolve("voodoo.url.txt"))

        val cfg = sortedMapOf(
                "InstanceType" to "OneSix",
                "OverrideCommands" to "true",
                "iconKey" to "default",
                "PreLaunchCommand" to "\$INST_JAVA -jar \"\$INST_DIR\\\\mmc-installer.jar\" --id \"\$INST_ID\" --inst \"\$INST_DIR\" --mc \"\$INST_MC_DIR\"",
                "name" to modpack.name //title
        )

        val cfgFile = instanceDir.resolve("instance.cfg")

        MMCUtil.writeCfg(cfgFile, cfg)

        // set minecraft and forge versions
        val mmcPackPath = instanceDir.resolve("mmc-pack.json")
        val mmcPack = MultiMCPack()
        MMCUtil.logger.info("forge version for build ${modpack.forge}")
        val (_, _, _, forgeVersion) = Forge.getForgeUrl(modpack.forge.toString(), modpack.mcVersion)
        MMCUtil.logger.info("forge version : $forgeVersion")
        mmcPack.components = listOf(
                PackComponent(
                        uid = "net.minecraft",
                        version = modpack.mcVersion,
                        important = true
                ),
                PackComponent(
                        uid = "net.minecraftforge",
                        version = forgeVersion,
                        important = true
                )
        )
        mmcPackPath.writeJson(mmcPack)

        val serverInstaller = instanceDir.resolve("mmc-installer.jar")
        val installer = downloadInstaller()
        installer.copyTo(serverInstaller)

        val packignore = instanceDir.resolve(".packignore")
        packignore.writeText(".minecraft\n")

        targetDir.mkdirs()
        val instanceZip = targetDir.resolve(modpack.name + ".zip")

        instanceZip.delete()
//        instanceDir.packToZip(instanceZip)
        packToZip(wrapperDir.toPath(), instanceZip.toPath())
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