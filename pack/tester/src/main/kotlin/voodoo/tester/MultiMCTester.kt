package voodoo.tester

import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import voodoo.data.DependencyType
import voodoo.data.Side
import voodoo.data.lock.LockPack
import voodoo.mmc.MMCSelectable
import voodoo.mmc.MMCUtil
import voodoo.util.Downloader
import voodoo.util.blankOr
import voodoo.util.withPool
import java.io.IOException

/**
 * Created by nikky on 06/05/18.
 * @author Nikky
 */

object MultiMCTester : AbstractTester() {
    override val label = "MultiMC Tester"

    override suspend fun execute(
        stopwatch: Stopwatch,
        modpack: LockPack,
        clean: Boolean
    ) = stopwatch {
        val folder = "voodoo_test_${modpack.id}"
        val title = "${modpack.title.blankOr ?: modpack.id} Test Instance"

        val cacheDir = directories.cacheHome
        val multimcDir = MMCUtil.findDir()
        val instanceDir = multimcDir.resolve("instances").resolve(folder)

        if (clean) {
            logger.info("cleaning old instance dir ($instanceDir)")
            instanceDir.deleteRecursively()
        }

        instanceDir.mkdirs()

        val minecraftDir = MMCUtil.installEmptyPack(
            title,
            folder,
            icon = modpack.iconFile,
            mcVersion = modpack.mcVersion,
            modloader = modpack.modloader,
            extraCfg = modpack.packOptions.multimcOptions.instanceCfg
        )

        val modsDir = minecraftDir.resolve("mods")
        modsDir.deleteRecursively()

        val minecraftSrcDir = modpack.sourceFolder
        Downloader.logger.info("copying files into minecraft dir ('$minecraftSrcDir' => '$minecraftDir')")
        if (minecraftSrcDir.exists()) {
            minecraftSrcDir.copyRecursively(minecraftDir, overwrite = true)
        }

        for (src in minecraftSrcDir.walkTopDown()) {
            if (src.name.endsWith(".lock.json")) continue
            if (src.name.endsWith(".entry.json")) continue

            val relPath = src.relativeTo(minecraftSrcDir)
            val dstFile = minecraftDir.resolve(relPath)

            if (dstFile.exists() && !(src.isDirectory && dstFile.isDirectory)) {
                val stillExists = when {
                    dstFile.isDirectory -> !dstFile.deleteRecursively()
                    else -> !dstFile.delete()
                }

                if (stillExists) {
                    throw FileAlreadyExistsException(
                        file = src,
                        other = dstFile,
                        reason = "The destination file already exists."
                    )
                }
            }

            if (src.isDirectory) {
                dstFile.mkdirs()
            } else {
                logger.debug("copying $src -> $dstFile")
                if (src.copyTo(dstFile, overwrite = true).length() != src.length()) {
                    throw IOException("Source file wasn't copied completely, length of destination file differs.")
                }
            }
        }

        logger.info("sorting client / server mods")
        for (file in minecraftDir.walkTopDown()) {
            when {
                // file.name.endsWith(".lock.json") -> file.delete()
                file.name == "_CLIENT" -> {
                    file.copyRecursively(file.parentFile, overwrite = true)
                    file.deleteRecursively()
                }
                file.name == "_SERVER" -> {
                    file.deleteRecursively()
                }
            }
        }

        val json = Json(JsonConfiguration(prettyPrint = true, encodeDefaults = false))
        val featureSerializer = MapSerializer(String.serializer(), Boolean.serializer())

        // read user input
        val featureJson = instanceDir.resolve("voodoo.features.json")
        val previousSelection = if (featureJson.exists()) {
            json.parse(featureSerializer, featureJson.readText())
        } else {
            mapOf<String, Boolean>()
        }
        val (optionals, reinstall) = MMCUtil.updateAndSelectFeatures(
            modpack.optionalEntries.filter{ it.side != Side.SERVER }.map {
                MMCSelectable(it)
            },
            previousSelection,
            name = modpack.title.blankOr ?: modpack.id,
            version = modpack.version,
            enableTimeout = false,
            installing = !featureJson.exists(),
            updateRequired = true
        )
        logger.debug("result: optionals: $optionals")
        if (!optionals.isEmpty()) {
            featureJson.createNewFile()
            featureJson.writeText(json.stringify(featureSerializer, optionals))
        }
        if (reinstall) {
            minecraftDir.deleteRecursively()
        }

        withPool { pool ->
            coroutineScope {
                modpack.entrySet.forEach { entry ->
                    if (entry.side == Side.SERVER) return@forEach
                    launch(pool + CoroutineName(entry.id)) {
                        val folder = minecraftDir.resolve(entry.serialFile).absoluteFile.parentFile

                        if(modpack.isEntryOptional(entry.id)) {
                            val selectedSelf = optionals[entry.id] ?: true
                            if (!selectedSelf) {
                                MMCUtil.logger.info("${entry.displayName} is disabled, skipping download")
                                return@launch
                            }
                            val matchedOptioalsList = modpack.optionalEntries.filter {
                                // check if entry is a dependency of any feature
                                modpack.isDependencyOf(
                                    entryId = entry.id,
                                    parentId = it.id,
                                    dependencyType = DependencyType.REQUIRED
                                )
                            }
                            logger.debug("${entry.id} is a dependency of ${matchedOptioalsList.map { it.id }}")
                            if (!matchedOptioalsList.isEmpty()) {
                                val selected = matchedOptioalsList.any { optionals[it.id] ?: false }
                                if (!selected) {
                                    MMCUtil.logger.info("${matchedOptioalsList.map { it.displayName } } is disabled, skipping download of ${entry.id}")
                                    return@launch
                                }
                            }
                        }

                        val provider = entry.provider()
                        val targetFolder = minecraftDir.resolve(folder)
                        val (url, file) = provider.download(
                            "download-${entry.id}".watch,
                            entry,
                            targetFolder,
                            cacheDir
                        )
                    }
                }
            }
        }

        logger.info("clearing serverside files and deleting lockfiles")
        for (file in minecraftDir.walkTopDown()) {
            when {
                file.name.endsWith(".lock.json") -> file.delete()
                file.name == "_CLIENT" -> {
                    file.copyRecursively(file.parentFile, overwrite = true)
                    file.deleteRecursively()
                }
                file.name == "_SERVER" -> {
                    file.deleteRecursively()
                }
            }
        }

//        MMCUtil.startInstance(title)
        MMCUtil.startInstance(folder)
    }
}