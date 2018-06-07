package voodoo

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import mu.KLogging
import org.apache.commons.codec.digest.DigestUtils
import voodoo.data.sk.SKPack
import voodoo.data.sk.task.TaskIf
import voodoo.mmc.MMCUtil.selectFeatures
import voodoo.mmc.data.MultiMCPack
import voodoo.mmc.data.PackComponent
import voodoo.util.*
import java.io.File


/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 */

object Hex : KLogging() {
    private val directories = Directories.get(moduleName = "hex")

    @JvmStatic
    fun main(vararg args: String) = mainBody {
        val arguments = Arguments(ArgParser(args))

        arguments.run {

            install(instanceId, instanceDir, minecraftDir)
        }

    }

    fun File.sha1Hex(): String? = DigestUtils.sha1Hex(this.inputStream())

    fun install(instanceId: String, instanceDir: File, minecraftDir: File) {

        val urlFile = instanceDir.resolve("voodoo.url.txt")
        val packUrl = urlFile.readText()
        val (_, _, result) = packUrl.httpGet()
//                .header("User-Agent" to useragent)
                .responseString()

        val modpack: SKPack = when (result) {
            is Result.Success -> {
                jsonMapper.readValue(result.value)
            }
            is Result.Failure -> {
                logger.error("${result.error} could not retrieve pack")
                return
            }
        }

        val oldpackFile = instanceDir.resolve("voodoo.modpack.json")
        val oldpack: SKPack? = if (!oldpackFile.exists())
            null
        else {
            val pack: SKPack = jsonMapper.readValue(oldpackFile)
            logger.info("loaded old pack ${pack.name} ${pack.version}")
            pack
        }

        if (oldpack != null) {
            if (oldpack.version == modpack.version) {
                logger.info("no update required ?")
//                return //TODO: make dialog close continue when no update is required ?
            }
        }

        val forgePrefix = "net.minecraftforge:forge:"
        val (_, _, forgeVersion) = modpack.versionManifest.libraries.find {
            it.name.startsWith(forgePrefix)
        }?.name.let { it ?: "::" }.split(':')

        logger.info("forge version is $forgeVersion")

        // read user input
        val featureJson = instanceDir.resolve("voodoo.features.json")
        val defaults = if (featureJson.exists()) {
            featureJson.readJson()
        } else {
            mapOf<String, Boolean>()
        }
        val features = selectFeatures(modpack.features, defaults)
        featureJson.writeJson(features)

        val cacheFolder = directories.cacheHome.resolve("hex")

        val objectsUrl = packUrl.substringBeforeLast('/') + "/" + modpack.objectsLocation

        val oldTasks = oldpack?.tasks?.toMutableList()

        // iterate new tasks
        for (task in modpack.tasks) {
            val whenTask = task.`when`
            if (whenTask != null) {
                val download = when (whenTask.`if`) {
                    TaskIf.requireAny -> {
                        whenTask.features.any { features[it] ?: false }
                    }
                }
                if (!download) {
                    logger.info("${whenTask.features} is disabled, skipping download")
                    continue
                }
            }


            val url = if (task.location.startsWith("http")) {
                task.location
            } else {
                "$objectsUrl/${task.location}"
            }
            val target = minecraftDir.resolve(task.to)
            val chunkedHash = task.hash.chunked(6).joinToString("/")

            val oldTask = oldTasks?.find { it.location == task.location }
            if (target.exists()) {
                if (oldTask != null) {
                    // file exists already and existed in the last version

                    if (task.userFile) {
                        if (oldTask.userFile) {
                            logger.info("task ${task.location} is a userfile, will not be modified")
                            oldTasks.remove(oldTask)
                            continue
                        }
                    }
                    if (oldTask.hash == task.hash) {
                        if (target.isFile && target.sha1Hex() == task.hash) {
                            logger.info("task ${task.location} file did not change and sha1 hash matches")
                            oldTasks.remove(oldTask)
                            continue
                        }
                    } else {
                        // mismatching hash.. override file
                        oldTasks.remove(oldTask)
                        target.delete()
                        target.parentFile.mkdirs()
                        target.download(url, cacheFolder.resolve(chunkedHash))
                    }
                } else {
                    // file exists but was not in the last version.. reset to make sure
                    target.delete()
                    target.parentFile.mkdirs()
                    target.download(url, cacheFolder.resolve(chunkedHash))
                }
            } else {
                // new file
                target.parentFile.mkdirs()
                target.download(url, cacheFolder.resolve(chunkedHash))
            }

            if (target.exists()) {
                val sha1 = target.sha1Hex()
                if (sha1 != task.hash) {
                    logger.error("hashes do not match")
                    logger.error(sha1)
                    logger.error(task.hash)
                }
            }
        }

        // iterate old
        oldTasks?.forEach { task ->
            val target = minecraftDir.resolve(task.to)
            target.delete()
        }


        // set minecraft and forge versions
        val mmcPackPath = instanceDir.resolve("mmc-pack.json")
        val mmcPack = if (mmcPackPath.exists()) {
            mmcPackPath.readJson()
        } else MultiMCPack()
        mmcPack.components = listOf(
                PackComponent(
                        uid = "net.minecraft",
                        version = modpack.gameVersion,
                        important = true
                ),
                PackComponent(
                        uid = "net.minecraftforge",
                        version = forgeVersion.substringAfter("${modpack.gameVersion}-"),
                        important = true
                )
        ) + mmcPack.components
        mmcPackPath.writeJson(mmcPack)

        oldpackFile.writeJson(modpack)
    }

    private class Arguments(parser: ArgParser) {
        val instanceId by parser.storing("--id",
                help = "\$INST_ID - ID of the instance")

        val instanceDir by parser.storing("--inst",
                help = "\$INST_DIR - absolute path of the instance") { File(this) }

        val minecraftDir by parser.storing("--mc",
                help = "\$INST_MC_DIR - absolute path of minecraft") { File(this) }
    }
}