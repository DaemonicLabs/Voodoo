package voodoo

import awaitObjectResponse
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import com.github.kittinunf.result.Result
import com.skcraft.launcher.model.modpack.Manifest
import com.xenomachina.argparser.ArgParser
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.joinAll
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.serialization.internal.BooleanSerializer
import kotlinx.serialization.internal.HashMapSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.serializer
import mu.KLogging
import org.apache.commons.codec.digest.DigestUtils
import voodoo.curse.CurseClient
import voodoo.mmc.MMCUtil.selectFeatures
import voodoo.mmc.data.MultiMCPack
import voodoo.mmc.data.PackComponent
import voodoo.util.Directories
import voodoo.util.blankOr
import voodoo.util.download
import voodoo.util.pool
import java.awt.Toolkit
import java.io.File
import java.util.Collections
import kotlin.system.exitProcess

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 */

object Hex : KLogging() {
    private val directories = Directories.get()
    val kit = Toolkit.getDefaultToolkit()

    @JvmStatic
    fun main(vararg args: String) = runBlocking {
        val arguments = Arguments(ArgParser(args))

        arguments.run {
            install(instanceId, instanceDir, minecraftDir)
        }
    }

    private fun File.sha1Hex(): String? = DigestUtils.sha1Hex(this.inputStream())

    private val json = JSON(indented = true)

    private suspend fun install(instanceId: String, instanceDir: File, minecraftDir: File) {
        logger.info("installing into $instanceId")
        val urlFile = instanceDir.resolve("voodoo.url.txt")
        val packUrl = urlFile.readText().trim()
        logger.info("pack url: $packUrl")

        val (request, response, result) = packUrl.httpGet()
            .header("User-Agent" to CurseClient.useragent)
            .awaitObjectResponse<Manifest>(kotlinxDeserializerOf())
        val modpack = when (result) {
            is Result.Success -> result.value
            is Result.Failure -> {
                logger.error(result.error.exception) { "could not retrieve pack, ${result.error}" }
                return
            }
        }

        val oldpackFile = instanceDir.resolve("voodoo.modpack.json")
        val oldpack: Manifest? = if (!oldpackFile.exists())
            null
        else {
            val pack: Manifest = json.parse(oldpackFile.readText())
            logger.info("loaded old pack ${pack.name} ${pack.version}")
            pack
        }

        var forceDisplay = false
        if (oldpack != null) {
            if (oldpack.version == modpack.version) {
                logger.info("no update required ? hold shift to force a update")
                delay(1000)
//                return //TODO: make dialog close continue when no update is required ?
//                if(kit.getLockingKeyState(KeyEvent.VK_CAPS_LOCK)) {
//                    forceDisplay = true
//                } else {
//                    exitProcess(0)
//                }
            }
        }

        val forgePrefix = "net.minecraftforge:forge:"
        var (_, _, forgeVersion) = modpack.versionManifest?.libraries?.find {
            it.name.startsWith(forgePrefix)
        }?.name.let { it ?: "::" }.split(':')
        if (forgeVersion.isBlank()) {
            logger.error("could not parse forge version in modpack")
            exitProcess(2)
        }
        while (forgeVersion.count { it == '-' } > 1) {
            forgeVersion = forgeVersion.substringBeforeLast("-")
        }
        logger.info("forge version is $forgeVersion")

        // read user input
        val featureJson = instanceDir.resolve("voodoo.features.json")
        val defaults = if (featureJson.exists()) {
            JSON.indented.parse(
                HashMapSerializer(String.serializer(), BooleanSerializer),
                featureJson.readText()
            )
        } else {
            mapOf<String, Boolean>()
        }
        val (features, reinstall) = selectFeatures(
            modpack.features, defaults, modpack.title.blankOr
                ?: modpack.name!!, modpack.version!!, forceDisplay = forceDisplay, updating = oldpack != null
        )
        featureJson.writeText(
            JSON.indented.stringify(
                HashMapSerializer(
                    String.serializer(),
                    BooleanSerializer
                ), features
            )
        )
        if (reinstall) {
            minecraftDir.deleteRecursively()
        }

        val objectsUrl = packUrl.substringBeforeLast('/') + "/" + modpack.objectsLocation

        val oldTaskList = Collections.synchronizedList(oldpack?.tasks?.toMutableList() ?: mutableListOf())

        runBlocking {
            val jobs = modpack.tasks.map { task ->
                launch(context = pool) {
                    val oldTask = oldTaskList.find { it.to == task.to }

                    val whenTask = task.conditionWhen
                    if (whenTask != null) {
                        val download = when (whenTask.ifSwitch) {
                            "requireAny" -> {
                                whenTask.features.any { feature -> features[feature] ?: false }
                            }
                            "requireAll" -> {
                                whenTask.features.all { feature -> features[feature] ?: false }
                            }
                            else -> false
                        }
                        if (!download) {
                            logger.info("${whenTask.features} is disabled, skipping download")
                            return@launch
                        }
                    }

                    val url = if (task.location.startsWith("http")) {
                        task.location
                    } else {
                        "$objectsUrl/${task.location}"
                    }
                    val target = minecraftDir.resolve(task.to)
                    val chunkedHash = task.hash.chunked(6).joinToString("/")
                    val cacheFolder = directories.cacheHome.resolve(chunkedHash)

                    if (target.exists()) {
                        if (oldTask != null) {
                            // file exists already and existed in the last version

                            if (task.isUserFile && oldTask.isUserFile) {
                                logger.info("task ${task.to} is a userfile, will not be modified")
                                oldTask.let { oldTaskList.remove(it) }
                                return@launch
                            }
                            if (oldTask.hash == task.hash && target.isFile && target.sha1Hex() == task.hash) {
                                logger.info("task ${task.to} file did not change and sha1 hash matches")
                                oldTask.let { oldTaskList.remove(it) }
                                return@launch
                            } else {
                                // mismatching hash.. override file
                                logger.info("task ${task.to} mismatching hash.. reset and override file")
                                oldTask.let { oldTaskList.remove(it) }
                                target.delete()
                                target.parentFile.mkdirs()
                                target.download(url, cacheFolder)
                            }
                        } else {
                            // file exists but was not in the last version.. reset to make sure
                            logger.info("task ${task.to} exists but was not in the last version.. reset to make sure")
                            target.delete()
                            target.parentFile.mkdirs()
                            target.download(url, cacheFolder)
                        }
                    } else {
                        // new file
                        logger.info("task ${task.to} creating new file")
                        target.parentFile.mkdirs()
                        target.download(url, cacheFolder)

                        oldTask?.let { oldTaskList.remove(it) }
                    }

                    if (target.exists()) {
                        val sha1 = target.sha1Hex()
                        if (sha1 != task.hash) {
                            logger.error("hashes do not match for task ${task.to}")
                            logger.error(sha1)
                            logger.error(task.hash)
                        } else {
                            logger.trace("task ${task.to} validated")
                        }
                    } else {
                        logger.error("file $target was not created")
                    }
                }
            }

            // iterate new tasks
            jobs.joinAll()
        }

        // iterate old
        oldTaskList.forEach { task ->
            val target = minecraftDir.resolve(task.to)
            logger.info("deleting $target")
            target.delete()
        }

        // set minecraft and forge versions
        val mmcPackPath = instanceDir.resolve("mmc-pack.json")
        val mmcPack = if (mmcPackPath.exists()) {
            json.parse(mmcPackPath.readText())
        } else MultiMCPack()
        mmcPack.components = listOf(
            PackComponent(
                uid = "net.minecraft",
                version = modpack.gameVersion!!,
                important = true
            ),
            PackComponent(
                uid = "net.minecraftforge",
                version = forgeVersion.substringAfter("${modpack.gameVersion}-"),
                important = true
            )
        ) + mmcPack.components
        mmcPackPath.writeText(json.stringify(mmcPack))

        oldpackFile.createNewFile()
        oldpackFile.writeText(json.stringify(modpack))
    }

    private class Arguments(parser: ArgParser) {
        val instanceId by parser.storing(
            "--id",
            help = "\$INST_ID - ID of the instance"
        )

        val instanceDir by parser.storing(
            "--inst",
            help = "\$INST_DIR - absolute path of the instance"
        ) { File(this) }

        val minecraftDir by parser.storing(
            "--mc",
            help = "\$INST_MC_DIR - absolute path of minecraft"
        ) { File(this) }
    }
}