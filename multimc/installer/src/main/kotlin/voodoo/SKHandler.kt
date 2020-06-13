package voodoo


import com.skcraft.launcher.model.modpack.FileInstall
import com.skcraft.launcher.model.modpack.Manifest
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.toList
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KLogging
import voodoo.mmc.MMCSelectable
import voodoo.mmc.MMCUtil.updateAndSelectFeatures
import voodoo.mmc.data.MultiMCPack
import voodoo.mmc.data.PackComponent
import voodoo.util.*
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 */

object SKHandler : KLogging() {
    private val directories = Directories.get(moduleName = "multimc")
//    val kit = Toolkit.getDefaultToolkit()

//    private fun File.sha1Hex(): String {
//        val digest = MessageDigest.getInstance("SHA-1")
//        return digest.digest(this.readBytes()).toHexString()
//    }

    private val json = Json(JsonConfiguration(prettyPrint = true, ignoreUnknownKeys = true, encodeDefaults = true))

    internal suspend fun install(modpack: Manifest, instanceId: String, instanceDir: File, minecraftDir: File) {
        logger.info("installing into $instanceId")
        val urlFile = instanceDir.resolve("voodoo.url.txt")
        val packUrl = urlFile.readText().trim()
        logger.info("pack url: $packUrl")

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
                logger.info("no update required ? hold shift to force a update (WIP)")
                // TODO: show timeout

//                delay(1000)
//                return //TODO: make dialog close continue when no update is required ?

                // get the key state somehow
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
            // TODO: also look for fabric version
            logger.error("could not parse forge version in modpack")
            exitProcess(2)
        }
        while (forgeVersion.count { it == '-' } > 1) {
            forgeVersion = forgeVersion.substringBeforeLast("-")
        }
        logger.info("forge version is $forgeVersion")

        val json =  Json(JsonConfiguration.Stable.copy(prettyPrint = true))
        val mapSerializer = MapSerializer(String.serializer(), Boolean.serializer())
        // read user input
        val featureJson = instanceDir.resolve("voodoo.features.json")
        val defaults = if (featureJson.exists()) {
            json.parse(mapSerializer, featureJson.readText())
        } else {
            mapOf()
        }
        val (features, reinstall, skipUpdate) = updateAndSelectFeatures(
            selectables = modpack.features.map {
                MMCSelectable(it.name, it.name, it.description, it.selected, it.recommendation)
            },
            previousSelection = defaults,
            name = modpack.title.blankOr
                ?: modpack.name!!,
            version = modpack.version!!,
            enableTimeout = true,
            installing = oldpack == null,
            updateRequired = oldpack?.version != modpack.version
        )
        if(skipUpdate) {
            return
        }
        featureJson.writeText(
            json.stringify(mapSerializer, features)
        )
        if (reinstall) {
            minecraftDir.deleteRecursively()
        }

        val objectsUrl = packUrl.substringBeforeLast('/') + "/" + modpack.objectsLocation

        val oldTaskList = (oldpack?.tasks ?: listOf())
        val uptodateTasks = Channel<FileInstall>(Channel.UNLIMITED)

        withPool { pool ->
            coroutineScope {
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
                                // file exists already and task existed in the last version

                                if (task.isUserFile && oldTask.isUserFile) {
                                    logger.info("task ${task.to} is a userfile, will not be modified")
                                    oldTask.let {
                                        uptodateTasks.send(it)
                                    }
                                    return@launch
                                }
                                if (oldTask.hash == task.hash && target.isFile && target.sha1Hex() == task.hash) {
                                    logger.info("task ${task.to} file did not change and sha1 hash matches")
                                    oldTask.let {
                                        uptodateTasks.send(it)
                                    }
                                    return@launch
                                } else {
                                    // mismatching hash.. override file
                                    logger.info("task ${task.to} mismatching hash.. reset and override file")
                                    target.delete()
                                    target.parentFile.mkdirs()
                                    target.download(
                                        url = url,
                                        cacheDir = cacheFolder,
                                        validator = { bytes, file ->
                                            val sha1 = bytes.sha1Hex()
                                            logger.info("comparing $sha1 == ${task.hash} of file: $file")
                                            sha1 == task.hash
                                        }
                                    )
                                    oldTask.let {
                                        uptodateTasks.send(it)
                                    }
                                }
                            } else {
                                // file exists but was not in the last version.. reset to make sure
                                logger.info("task ${task.to} exists but was not in the last version.. reset to make sure")
                                target.delete()
                                target.parentFile.mkdirs()
                                target.download(
                                    url = url,
                                    cacheDir = cacheFolder,
                                    validator = { bytes, file ->
                                        val sha1 = bytes.sha1Hex()
                                        logger.info("comparing $sha1 == ${task.hash} of file: $file")
                                        sha1 == task.hash
                                    }
                                )
                            }
                        } else {
                            // new file
                            logger.info("task ${task.to} creating new file")
                            target.parentFile.mkdirs()
                            target.download(url, cacheFolder)

                            oldTask?.let {
                                uptodateTasks.send(it)
                            }
                        }

                        if (target.exists()) {
                            val sha1 = target.sha1Hex()
                            if (sha1 != task.hash) {
                                logger.error("hashes do not match for task ${task.to}")
                                error("hashes for ${task.to} do not match, expected: ${task.hash} actual: $sha1")
                            } else {
                                logger.trace("task ${task.to} validated")
                            }
                        } else {
                            logger.error("file $target was not created")
                        }
                    }
                }
            }
        }

        uptodateTasks.close()
        val toRemove = (oldpack?.tasks ?: listOf()) - uptodateTasks.toList()
        logger.info("files to delete: ${toRemove.map { it.to }}")

        // iterate old and delete
        toRemove.forEach { task ->
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
}