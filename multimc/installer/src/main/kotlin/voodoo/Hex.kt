package voodoo

import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import com.github.kittinunf.result.Result
import com.skcraft.launcher.model.modpack.FileInstall
import com.skcraft.launcher.model.modpack.Manifest
import com.xenomachina.argparser.ArgParser
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.internal.BooleanSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.map
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

    private val json = Json(indented = true, strictMode = false, encodeDefaults = true)

    @UseExperimental(ObsoleteCoroutinesApi::class)
    private suspend fun install(instanceId: String, instanceDir: File, minecraftDir: File) {
        logger.info("installing into $instanceId")
        val urlFile = instanceDir.resolve("voodoo.url.txt")
        val packUrl = urlFile.readText().trim()
        logger.info("pack url: $packUrl")

        val (request, response, result) = packUrl.httpGet()
            .header("User-Agent" to CurseClient.useragent)
            .awaitObjectResponseResult(kotlinxDeserializerOf(loader = Manifest.serializer(), json = json))
        val modpack: Manifest = when (result) {
            is Result.Success -> {
                result.value
            }
            is Result.Failure -> {
                logger.error("packUrl: $packUrl")
                logger.error("cUrl: ${request.cUrlString()}")
                logger.error("response: $response")
                logger.error(result.error.exception) { "could not retrieve pack, ${result.error}" }
                return
            }
        }

        val oldpackFile = instanceDir.resolve("voodoo.modpack.json")
        val oldpack: Manifest? = oldpackFile.takeIf { it.exists() }
            ?.let { packFile ->
                try {
                    json.parse(Manifest.serializer(), packFile.readText())
                        .also { pack ->
                            logger.info("loaded old pack ${pack.name} ${pack.version}")
                        }
                } catch (e: IllegalArgumentException) {
                    logger.error(e.message)
                    e.printStackTrace()
                    oldpackFile.delete()
                    null
                }
            }

        var forceDisplay = false
        if (oldpack != null) {
            if (oldpack.version == modpack.version) {
                logger.info("no update required ? hold shift to force a update")
                // TODO: show timeout

                delay(1000)
                return //TODO: make dialog close continue when no update is required ?
//                if(kit.getLockingKeyState(KeyEvent.VK_CAPS_LOCK)) {
//                    forceDisplay = true
//                } else {
//                    return
//                }
            } else {
                logger.info("old pack version mismatched")
            }
        } else {
            logger.info("no old pack found")
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

        val mapSerializer = (String.serializer() to BooleanSerializer).map
        // read user input
        val featureJson = instanceDir.resolve("voodoo.features.json")
        val defaults = if (featureJson.exists()) {
            Json.indented.parse(mapSerializer, featureJson.readText())
        } else {
            mapOf()
        }
        val (features, reinstall) = selectFeatures(
            modpack.features, defaults, modpack.title.blankOr
                ?: modpack.name!!, modpack.version!!, forceDisplay = forceDisplay, updating = oldpack != null
        )
        featureJson.writeText(
            Json.indented.stringify(mapSerializer, features)
        )
        if (reinstall) {
            minecraftDir.deleteRecursively()
        }

        val objectsUrl = packUrl.substringBeforeLast('/') + "/" + modpack.objectsLocation

        val oldTaskList = (oldpack?.tasks?.toMutableList() ?: mutableListOf()) as List<FileInstall>

        coroutineScope {
            val oldTasksRemover = actor<FileInstall> {
                oldTaskList as MutableList
                for (msg in channel) {
                    logger.info("removing ${msg.to}")
                    oldTaskList.remove(msg)
                }
            }

            for (task in modpack.tasks) {
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
                                oldTask.let {
                                    oldTasksRemover.send(it)
                                }
                                return@launch
                            }
                            if (oldTask.hash == task.hash && target.isFile && target.sha1Hex() == task.hash) {
                                logger.info("task ${task.to} file did not change and sha1 hash matches")
                                oldTask.let {
                                    oldTasksRemover.send(it)
                                }
                                return@launch
                            } else {
                                // mismatching hash.. override file
                                logger.info("task ${task.to} mismatching hash.. reset and override file")
                                oldTask.let {
                                    oldTasksRemover.send(it)
                                }
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

                        oldTask?.let {
                            oldTasksRemover.send(it)
                        }
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
                }.join()
            }
            oldTasksRemover.close()
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
            json.parse(MultiMCPack.serializer(), mmcPackPath.readText())
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
        mmcPackPath.writeText(json.stringify(MultiMCPack.serializer(), mmcPack))

        oldpackFile.createNewFile()
        oldpackFile.writeText(json.stringify(Manifest.serializer(), modpack))
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