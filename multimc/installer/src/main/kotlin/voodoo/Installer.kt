package voodoo


import Modloader
import com.skcraft.launcher.model.modpack.Recommendation
import com.xenomachina.argparser.ArgParser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.toList
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KLogging
import moe.nikky.voodoo.format.modpack.Manifest
import moe.nikky.voodoo.format.modpack.entry.FileInstall
import moe.nikky.voodoo.format.modpack.entry.Side
import voodoo.mmc.MMCSelectable
import voodoo.mmc.MMCUtil
import voodoo.mmc.MMCUtil.updateAndSelectFeatures
import voodoo.mmc.data.MultiMCPack
import voodoo.mmc.data.PackComponent
import voodoo.multimc.installer.GeneratedConstants
import voodoo.multimc.installer.ModuleBootstrapMultimcInstaller
import voodoo.util.*
import voodoo.util.maven.MavenUtil
import java.io.File
import java.io.IOException

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 */

object Installer : KLogging() {
    private val directories = Directories.get(moduleName = "multimc")
//    val kit = Toolkit.getDefaultToolkit()

    @JvmStatic
    fun main(vararg args: String) = runBlocking {
        System.setProperty(DEBUG_PROPERTY_NAME, "on")

        val arguments = Arguments(ArgParser(args))

        arguments.run {
//            selfupdate(instanceDir)
            install(instanceId, instanceDir, minecraftDir)
        }
    }

    private suspend fun selfupdate(instanceDir: File) {
        val voodooFolder = instanceDir.resolve(".voodoo").apply {mkdirs()}
        val toDeleteFile = voodooFolder.resolve("to-delete.txt")
        toDeleteFile.parentFile.mkdirs()
        if(toDeleteFile.exists()) {
            instanceDir.resolve(toDeleteFile.readText())
                .takeIf { it.exists() }
                ?.delete()
        }

        val cfgFile = instanceDir.resolve("instance.cfg")
        val cfg = MMCUtil.readCfg(cfgFile)

        val oldPreLaunchCommand = cfg["PreLaunchCommand"]

        if(cfg["OverrideCommands"] == "true" && oldPreLaunchCommand != null) {
            val installerFilename = "mmc-installer-${ModuleBootstrapMultimcInstaller.FULL_VERSION}"

            val currentJarFilePath = oldPreLaunchCommand.substringAfter("\"\$INST_JAVA\" -jar \"\$INST_DIR/").substringBefore("\"")
            val currentJarFile = instanceDir.resolve(currentJarFilePath)

            if(currentJarFile.exists() && currentJarFile.name != installerFilename) {
                val installerFile = MavenUtil.downloadArtifact(
                    mavenUrl = GeneratedConstants.MAVEN_URL,
                    group = GeneratedConstants.MAVEN_GROUP,
                    artifactId = "bootstrap-multimc-installer",
                    version = ModuleBootstrapMultimcInstaller.FULL_VERSION,
                    classifier = GeneratedConstants.MAVEN_SHADOW_CLASSIFIER,
                    outputFile = instanceDir.resolve(installerFilename),
                    outputDir = instanceDir
                )
                val preLaunchCommand =
                    "\"\$INST_JAVA\" -jar \"\$INST_DIR/${installerFile.toRelativeString(instanceDir).replace('\\', '/')}\" --id \"\$INST_ID\" --inst \"\$INST_DIR\" --mc \"\$INST_MC_DIR\""

                cfg["PreLaunchCommand"] = preLaunchCommand

                MMCUtil.writeCfg(cfgFile, cfg)

//                toDeleteFile.writeText(currentJarFile.toRelativeString(instanceDir))
            }
        } else {
            logger.info("commands are not enabled, not updating bootstrapper")
        }
    }

    private val json = Json(JsonConfiguration(prettyPrint = true, ignoreUnknownKeys = true, encodeDefaults = true))

    private suspend fun install(instanceId: String, instanceDir: File, minecraftDir: File) {
        logger.info("installing into $instanceId")
        val urlFile = instanceDir.resolve("voodoo.url.txt")
        val packUrl = urlFile.readText().trim()
        logger.info("pack url: $packUrl")

        val response = withContext(Dispatchers.IO) {
            try {
                client.get<HttpResponse> {
                    url(packUrl)
                    header(HttpHeaders.UserAgent, useragent)
                }
            } catch (e: IOException) {
                logger.error("packUrl: $packUrl")
                logger.error(e) { "unable to get pack from $packUrl" }
                error("failed to get $packUrl")
            }
        }
        if (!response.status.isSuccess()) {
            logger.error { "$packUrl returned ${response.status}" }
            error("failed with ${response.status}")
        }

        val jsonString = response.readText()


        // try skcraft handling
        try {
            val skcraftManifest = json.parse(com.skcraft.launcher.model.modpack.Manifest.serializer(), jsonString)
            return SKHandler.install(
                skcraftManifest,
                instanceId,
                instanceDir,
                minecraftDir
            )
        } catch (e: SerializationException) {
            logger.info ("not a skcraft manifest")
        }

        // TODO: load correct installer for formatVersion

        val modpack = json.parse(Manifest.serializer(), jsonString)

        // TODO: implement version listing later
//        try {
//            try {
//                // look up versions from a listing
//                val versionListing = json.parse(MapSerializer(String.serializer(), String.serializer()), jsonString)
//                val targetVersion = instanceDir.resolve("channel.txt").takeIf { it.exists() }?.readText()?.trim() ?: "latest"
//                val versionPointer = versionListing[targetVersion]
//
//                val packUrl = packUrl.substringBeforeLast('/') + "/" + versionPointer
//                val response = withContext(Dispatchers.IO) {
//                    try {
//                        client.get<HttpResponse> {
//                            url(packUrl)
//                            header(HttpHeaders.UserAgent, useragent)
//                        }
//                    } catch (e: IOException) {
//                        logger.error("packUrl: $packUrl")
//                        logger.error(e) { "unable to get pack from $packUrl" }
//                        error("failed to get $packUrl")
//                    }
//                }
//                if (!response.status.isSuccess()) {
//                    logger.error { "$packUrl returned ${response.status}" }
//                    error("failed with ${response.status}")
//                }
//                json.parse(Manifest.serializer(), response.readText())
//            } catch (e: SerializationException) {
//                json.parse(Manifest.serializer(), jsonString)
//            }
//        } catch (e: SerializationException) {
//            return SKHandler.install(
//                json.parse(com.skcraft.launcher.model.modpack.Manifest.serializer(), jsonString),
//                instanceId,
//                instanceDir,
//                minecraftDir
//            )
//        }

        val oldpackFile = instanceDir.resolve("voodoo.modpack.json")
        val oldpack: Manifest? = oldpackFile.takeIf { it.exists() }
            ?.let { packFile ->
                try {
                    json.parse(Manifest.serializer(), packFile.readText())
                        .also { pack ->
                            logger.info("loaded old pack ${pack.id} ${pack.version}")
                        }
                } catch (e: IllegalArgumentException) {
                    logger.error(e.message)
                    e.printStackTrace()
                    oldpackFile.delete()
                    null
                } catch (e: SerializationException) {
                    logger.error(e.message)
                    e.printStackTrace()
                    oldpackFile.delete()
                    null
                }
            }

        var forceDisplay = false
        if (oldpack != null) {
            if (oldpack.version == modpack.version) {
                logger.info("no update required")
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

//        val fabricPrefix = "net.fabricmc.fabric-loader"
//        val forgePrefix = "net.minecraftforge:forge:"
//        var (_, _, forgeVersion) = modpack.versionManifest?.libraries?.find {
//            it.name.startsWith(forgePrefix)
//        }?.name.let { it ?: "::" }.split(':')
//        if (forgeVersion.isBlank()) {
//            // TODO: also look for fabric version
//            logger.error("could not parse forge version in modpack")
//            exitProcess(2)
//        }
//        while (forgeVersion.count { it == '-' } > 1) {
//            forgeVersion = forgeVersion.substringBeforeLast("-")
//        }
        logger.info("modloader is ${modpack.modLoader}")

        val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true))
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
                MMCSelectable(it.name, it.name, it.description, it.selected, it.recommendation?.let { r -> Recommendation.valueOf(r.name) })
            },
            previousSelection = defaults,
            name = modpack.title.blankOr
                ?: modpack.id,
            version = modpack.version!!,
            forceDisplay = forceDisplay,
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
                    if(task.side == Side.SERVER) continue
                    launch(context = pool) {
                        val oldTask = oldTaskList.find { it.to == task.to }

                        val whenTask = task.condition
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
                        val chunkedHash = task.hash.substringAfter(':').chunked(6).joinToString("/")
                        val cacheFolder = directories.cacheHome.resolve("HEX").resolve(chunkedHash)

                        if (target.exists()) {
                            if (oldTask != null) {
                                // file exists already and existed in the last version

                                if (task.userFile && oldTask.userFile) {
                                    logger.info("task ${task.to} is a userfile, will not be modified")
                                    oldTask.let {
                                        uptodateTasks.send(it)
                                    }
                                    return@launch
                                }
                                if (oldTask.hash == task.hash && target.isFile && target.sha256Hex() == task.hash.substringAfter(':')) {
                                    logger.info("task ${task.to} file did not change and sha256 hash matches")
                                    oldTask.let {
                                        uptodateTasks.send(it)
                                    }
                                    return@launch
                                } else {
                                    // mismatching hash.. override file
                                    logger.info("task ${task.to} mismatching hash.. reset and override file")
                                    oldTask.let {
                                        uptodateTasks.send(it)
                                    }
                                    target.delete()
                                    target.parentFile.mkdirs()
                                    target.download(
                                        url = url,
                                        cacheDir = cacheFolder,
                                        validator = { file ->
                                            val sha256 = file.sha256Hex()
                                            logger.info("comparing $sha256 == ${task.hash} of file: $file")
                                            sha256 == task.hash.substringAfter(':')
                                        }
                                    )
                                }
                            } else {
                                // file exists but was not in the last version.. reset to make sure
                                logger.info("task ${task.to} exists but was not in the last version.. reset to make sure")
                                target.delete()
                                target.parentFile.mkdirs()

                                target.download(
                                    url = url,
                                    cacheDir = cacheFolder,
                                    validator = { file ->
                                        val sha256 = file.sha256Hex()
                                        logger.info("comparing $sha256 == ${task.hash} of file: $file")
                                        sha256 == task.hash.substringAfter(':')
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
                            val sha256 = target.sha256Hex()
                            if (sha256 != task.hash.substringAfter(':')) {
                                logger.error("hashes do not match for task ${task.to}")
                                error("hashes for ${task.to} do not match, expected: ${task.hash} actual: sha-256:$sha256")
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
        val toRemove = (oldpack?.tasks?.filterNot { it.side == Side.SERVER } ?: listOf()) - uptodateTasks.toList()
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

        val modloaderComponents = when (val modloader = modpack.modLoader) {
            is Modloader.Forge -> {
                listOf(
                    PackComponent(
                        uid = "net.minecraftforge",
                        version = modloader.forgeVersion,
                        important = true
                    )
                )
            }
            is Modloader.Fabric -> {
                listOf(
                    PackComponent(
                        uid = "net.fabricmc.intermediary",
                        version = modloader.intermediateMappings,
                        important = true
                    ),
                    PackComponent(
                        uid = "net.fabricmc.fabric-loader",
                        version = modloader.loader,
                        important = true
                    )
                )
            }
            else -> listOf()
        }

        mmcPack.components = listOf(
            PackComponent(
                uid = "net.minecraft",
                version = modpack.gameVersion,
                important = true
            )
        ) + modloaderComponents + mmcPack.components
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