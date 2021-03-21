package voodoo.multimc

import Modloader
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.toList
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import moe.nikky.voodoo.format.VersionsList
import moe.nikky.voodoo.format.modpack.Manifest
import moe.nikky.voodoo.format.modpack.Recommendation
import moe.nikky.voodoo.format.modpack.entry.FileInstall
import moe.nikky.voodoo.format.modpack.entry.Side
import mu.KotlinLogging
import voodoo.mmc.MMCSelectable
import voodoo.mmc.MMCUtil
import voodoo.mmc.MMCUtil.updateAndSelectFeatures
import voodoo.mmc.data.MultiMCPack
import voodoo.mmc.data.PackComponent
import voodoo.multimc.installer.GeneratedConstants
import voodoo.util.*
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Paths
import kotlin.system.exitProcess

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 */

object Installer {
    private val logger = KotlinLogging.logger {}
    private val directories = Directories.get(moduleName = "MULTIMC")
    val cacheHome by lazy { directories.cacheHome }
//    val kit = Toolkit.getDefaultToolkit()

    private suspend fun selfupdate(instanceDir: File, installerUrl: String, phase: Phase): File? {
        logger.info { "installer url from modpack: $installerUrl" }
        val voodooFolder = instanceDir.resolve(".voodoo").apply { mkdirs() }
        // TODO: cleanup old files smarter
//        voodooFolder.listFiles().forEach {
//            it.delete()
//        }
        val toDeleteFile = voodooFolder.resolve("to-delete.txt")
        toDeleteFile.parentFile.mkdirs()
        val currentJarFile = File(Installer::class.java.protectionDomain.codeSource.location.toURI())
        logger.info { "version: ${GeneratedConstants.FULL_VERSION}" }
        logger.info { "currentJarFile: ${currentJarFile.toRelativeString(instanceDir)}" }
        logger.info { "sha256: ${currentJarFile.sha256Hex()}" }
        when (phase) {
            Phase.POST -> {
                if (toDeleteFile.exists()) {
                    val success = toDeleteFile.readLines().all { toDelete ->
                        logger.info { "deleting $toDelete" }
                        instanceDir.resolve(toDelete)
                            .takeIf { it.exists() }
                            ?.delete()
                            ?: true
                    }
                    if (success) {
                        logger.info { "all files sucessfully deleted" }
                        toDeleteFile.delete()
                    }
                }

                // copy post.jar to multinc-installer.jar
                currentJarFile.copyTo(voodooFolder.resolve("multimc-installer.jar"), overwrite = true)
                exitProcess(0)
            }
            Phase.PRE -> {
                // always copy current jar to post.jar
                val postFile = voodooFolder.resolve("post.jar")
                if(postFile.exists()) {
                    postFile.delete()
                }
                currentJarFile.copyTo(postFile, overwrite = true)

                // checking for update
                val checksumUrl = installerUrl + ".sha256"
                logger.info { "downloading sha256 from $checksumUrl" }
                val newSha256 = if(installerUrl.startsWith("file:")) {
                    val sourceFile = File(checksumUrl.substringAfter("file:"))
                    sourceFile.readText()
                } else {
                    useClient { client ->
                        client.get<String>(urlString = checksumUrl)
                    }
                }

                if (currentJarFile.exists() && currentJarFile.sha256Hex().equals(newSha256, ignoreCase = true)) {
                    logger.info { "file exists and sha256 matches" }
                    return null
                }

                logger.info { "downloading $installerUrl" }
                if(installerUrl.startsWith("file:")) {
                    val sourceFile = File(installerUrl.substringAfter("file:"))
                    sourceFile.copyTo(postFile, overwrite = true)
                } else {
                    postFile.download(installerUrl, cacheHome)
                }
                val newFile = cacheHome.resolve("tmp.jar")
                postFile.copyTo(newFile, overwrite = true)
                return newFile
            }
            else -> {
                return null
            }
        }
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    internal suspend fun install(
        instanceId: String,
        instanceDir: File,
        minecraftDir: File,
        phase: Phase
    ) {
        logger.info { "installing into $instanceId" }
        val urlFile = instanceDir.resolve("voodoo.url.txt")
        val packUrl = urlFile.readText().trim()
        logger.info { "pack url: $packUrl" }

        val versionListing = withContext(Dispatchers.IO) versionListing@{
            try {
                if(packUrl.startsWith("file:")) {
                    return@versionListing json.decodeFromString(VersionsList.serializer(), File(packUrl.substringAfter("file:")).readText())
                }
                useClient { client ->
                    client.get<HttpResponse>(packUrl)
                }.let { response ->
                    if (!response.status.isSuccess()) {
                        logger.error { "$packUrl returned ${response.status}" }
                        error("failed with ${response.status}")
                    }

                    val jsonString = response.readText()

                    json.decodeFromString(VersionsList.serializer(), jsonString)
                }
            } catch (e: IOException) {
                logger.error { "packUrl: $packUrl" }
                logger.error(e) { "unable to get pack from $packUrl" }
                error("failed to get $packUrl")
            }
        }

        // TODO: pick version or update channel
        //TODO: load last version or pick
        val versionChoiceFile = instanceDir.resolve("voodoo.version.txt")
        var selectedVersionKey = if(versionChoiceFile.exists()) {
            versionChoiceFile.readLines().first()
        } else null

        if(selectedVersionKey == null && versionListing.versions.keys.size == 1) {
            selectedVersionKey = versionListing.versions.keys.first()
        } else if(selectedVersionKey == null || selectedVersionKey !in versionListing.versions) {
            selectedVersionKey = MMCUtil.selectVersion(
                versions = versionListing.versions.toSortedMap()
            )
            versionChoiceFile.writeText(selectedVersionKey)
        }
        val selectedVersion = versionListing.versions.getValue(selectedVersionKey)


        val modpack = withContext(Dispatchers.IO) modpack@{
            try {
                if(packUrl.startsWith("file:")) {
                    val manifestFile = File(packUrl.substringAfter("file:")).parentFile.resolve(selectedVersion.location)
                    return@modpack json.decodeFromString(Manifest.serializer(), manifestFile.readText())
                }
                val manifestUrl = URI(packUrl).resolve(selectedVersion.location).toURL()
                useClient { client ->
                    client.get<HttpResponse>(manifestUrl)
                }.let { response ->
                    if (!response.status.isSuccess()) {
                        logger.error { "$packUrl returned ${response.status}" }
                        error("failed with ${response.status}")
                    }

                    val jsonString = response.readText()

                    json.decodeFromString(Manifest.serializer(), jsonString)
                }
            } catch (e: IOException) {
                logger.error("packUrl: $packUrl")
                logger.error(e) { "unable to get pack from $packUrl" }
                error("failed to get $packUrl")
            }
        }
        val newJar = selfupdate(
            instanceDir,
            packUrl.substringBeforeLast('/') + "/" + modpack.installerLocation,
            phase
        )
        if (newJar != null) {
            val java = Paths.get(System.getProperty("java.home"), "bin", "java").toFile().path
            val workingDir = File(System.getProperty("user.dir"))
            val debugArgs: Array<String>

            debugArgs = if (System.getProperty(DEBUG_PROPERTY_NAME) != null) {
                arrayOf("-D$DEBUG_PROPERTY_NAME=$DEBUG_PROPERTY_VALUE_ON")
            } else {
                arrayOf()
            }

            val args = arrayOf<String>(
                java,
                *debugArgs,
                "-jar",
                newJar.path,
                "--id",
                instanceId,
                "--inst",
                instanceDir.path,
                "--mc",
                minecraftDir.path,
                "--phase",
                phase.name
            )

            logger.info { "Executing ${args.joinToString()}" }
            System.err.println("\nrebooting...\n\n\n")
            val exitStatus = ProcessBuilder(*args)
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
                .waitFor()
            exitProcess(exitStatus)
        }

        if(phase == Phase.POST) {
            return
        }

        val oldpackFile = instanceDir.resolve("voodoo.modpack.json")
        val oldpack: Manifest? = oldpackFile.takeIf { it.exists() }
            ?.let { packFile ->
                try {
                    json.decodeFromString(Manifest.serializer(), packFile.readText())
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

        logger.info("modloader is ${modpack.modLoader}")

        val json = Json {
            prettyPrint = true
        }
        val mapSerializer = MapSerializer(String.serializer(), Boolean.serializer())
        // read user input
        val featureJson = instanceDir.resolve("voodoo.features.json")
        val defaults = if (featureJson.exists()) {
            json.decodeFromString(mapSerializer, featureJson.readText())
        } else {
            mapOf()
        }
        val (features, reinstall, skipUpdate) = updateAndSelectFeatures(
            selectables = modpack.features.filter { it.side != Side.SERVER }.map {
                MMCSelectable(
                    it.name,
                    it.name,
                    it.description,
                    it.selected,
                    it.recommendation?.let { r -> Recommendation.valueOf(r.name) })
            },
            previousSelection = defaults,
            name = modpack.title.blankOr
                ?: modpack.id,
            version = modpack.version!!,
            enableTimeout = true,
            installing = oldpack == null,
            updateRequired = oldpack?.version != modpack.version
        )
        if (skipUpdate) {
            return
        }
        featureJson.writeText(
            json.encodeToString(mapSerializer, features)
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
                    if (task.side == Side.SERVER) continue
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
                        val cacheFolder = cacheHome.resolve("HEX").resolve(chunkedHash)

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
                                if (oldTask.hash == task.hash && target.isFile && target.sha256Hex() == task.hash.substringAfter(
                                        ':'
                                    )
                                ) {
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
                                    if(url.startsWith("file:")) {
                                        val sourceFile = File(url.substringAfter("file:"))
                                        sourceFile.copyTo(target, overwrite = true)
                                    } else {
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

                                }
                            } else {
                                // file exists but was not in the last version.. reset to make sure
                                logger.info("task ${task.to} exists but was not in the last version.. reset to make sure")
                                target.delete()
                                target.parentFile.mkdirs()

                                if(url.startsWith("file:")) {
                                    val sourceFile = File(url.substringAfter("file:"))
                                    sourceFile.copyTo(target, overwrite = true)
                                } else {
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
                            }
                        } else {
                            // new file
                            logger.info("task ${task.to} creating new file")
                            target.parentFile.mkdirs()

                            if(url.startsWith("file:")) {
                                val sourceFile = File(url.substringAfter("file:"))
                                sourceFile.copyTo(target, overwrite = true)
                            } else {
                                target.download(url, cacheFolder)
                            }

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
        val newTargets = uptodateTasks.toList().map { it.to }
        val toRemove = oldTaskList.filter { it.side != Side.SERVER }.filter { it.to !in  newTargets}
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
            json.decodeFromString(MultiMCPack.serializer(), mmcPackPath.readText())
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

        val jsonWithDefaults = Json(json) {
            encodeDefaults = true
        }
        mmcPackPath.writeText(jsonWithDefaults.encodeToString(MultiMCPack.serializer(), mmcPack))

        oldpackFile.createNewFile()
        oldpackFile.writeText(json.encodeToString(Manifest.serializer(), modpack))
    }
}