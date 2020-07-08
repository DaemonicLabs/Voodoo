package voodoo.pack

import com.eyeem.watchadoin.Stopwatch
import com.skcraft.launcher.builder.FeaturePattern
import com.skcraft.launcher.builder.PackageBuilder
import com.skcraft.launcher.model.ExtendedFeaturePattern
import com.skcraft.launcher.model.SKModpack
import com.skcraft.launcher.model.modpack.Feature
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import Modloader
import voodoo.data.DependencyType
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.forge.ForgeUtil
import voodoo.memoize
import voodoo.pack.sk.SKLocation
import voodoo.pack.sk.SKPackages
import voodoo.pack.sk.SKWorkspace
import voodoo.pack.sk.SkPackageFragment
import voodoo.provider.Providers
import voodoo.util.*
import java.io.File
import java.net.URI
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

object SKPack : AbstractPack("sk") {

    override val label = "SK Pack"

    override fun File.getOutputFolder(id: String): File = resolve("sk")

    override suspend fun pack(
        stopwatch: Stopwatch,
        modpack: LockPack,
        output: File,
        uploadBaseDir: File,
        clean: Boolean
    ) = stopwatch {
        val cacheDir = directories.cacheHome
        val workspaceDir = modpack.rootFolder.resolve("build").resolve("workspace").absoluteFile
        val modpackDir = workspaceDir.resolve(modpack.id)

        val skSrcFolder = modpackDir.resolve("src")
        logger.info("cleaning modpack directory $skSrcFolder")
        skSrcFolder.deleteRecursively()
        logger.info("copying files into src ${modpack.sourceFolder}")
        val packSrc = modpack.sourceFolder
        if (skSrcFolder.startsWith(packSrc)) {
            throw IllegalStateException("cannot copy parent rootFolder '$packSrc' into subfolder '$skSrcFolder'")
        }
        if (packSrc.exists()) {
            logger.debug("cp -r $packSrc $skSrcFolder")
            packSrc.copyRecursively(skSrcFolder, overwrite = true)
            skSrcFolder.walkBottomUp().forEach {
                if (it.name.endsWith(".entry.json") || it.name.endsWith(".lock.json") || it.name.endsWith(".lock.pack.json"))
                    it.delete()
                if (it.isDirectory && it.listFiles().isEmpty()) {
                    it.delete()
                }
            }
        } else {
            logger.warn("minecraft directory $packSrc does not exist")
        }

        for (file in skSrcFolder.walkTopDown()) {
            when {
                file.name == "_SERVER" -> file.deleteRecursively()
                file.name == "_CLIENT" -> file.renameTo(file.parentFile)
            }
        }

        val loadersFolder = modpackDir.resolve("loaders")
        logger.info("cleaning loaders $loadersFolder")
        loadersFolder.deleteRecursively()

        withPool { pool ->
            coroutineScope {
                // download forge
                (modpack.modloader as? Modloader.Forge)?.apply {
                    val (forgeUrl, forgeFileName, _, forgeVersion) = ForgeUtil.forgeVersionOf(this)
                    val forgeFile = loadersFolder.resolve(forgeFileName)
                    forgeFile.download(forgeUrl, cacheDir.resolve("FORGE").resolve(forgeVersion))
                } ?: logger.warn { "no forge configured" }

                val modsFolder = skSrcFolder.resolve("mods")
                logger.info("cleaning mods $modsFolder")
                modsFolder.deleteRecursively()

                // download entries
                val targetFiles = "download entries".watch {
                    val deferredFiles: List<Deferred<Pair<String, File>>> = modpack.entrySet.map { entry ->
                        async(context = pool + CoroutineName("download-${entry.id}")) {
                            val provider = Providers[entry.provider]

                            val targetFolder = skSrcFolder.resolve(entry.serialFile).parentFile

                            val (url, file) = provider.download(
                                "download-${entry.id}".watch,
                                entry,
                                targetFolder,
                                cacheDir
                            )
                            if (url != null
                                && ((entry is LockEntry.Direct && entry.useUrlTxt) ||
                                        (entry is LockEntry.Curse && entry.useUrlTxt))
                            ) {
                                val urlTxtFile = targetFolder.resolve(file.name + ".url.txt")
                                urlTxtFile.writeText(url)
                            }
                            //                println("done: ${entry.id} $file")
                            entry.id to file // serialFile.relativeTo(skSrcFolder
                        }.also {
                            logger.info("started job: download '${entry.id}'")
                            delay(10)
                        }
                    }
                    delay(10)
                    logger.info("waiting for file jobs to finish")
                    deferredFiles.awaitAll().toMap()
                }

                val features = mutableListOf<ExtendedFeaturePattern>()

                "resolve feature dep".watch {
                    for (entry in modpack.entrySet) {
                        resolveFeatureDependencies(
                            "${entry.id}-resolveFeatureDep".watch,
                            modpack,
                            entry,
                            modpack.findEntryById(entry.id)?.displayName
                                ?: throw NullPointerException("cannot find lockentry for ${entry.id}"),
                            features
                        )
                    }
                }

                "resolve features".watch {
                    // resolve features
                    for (feature in features) {
                        logger.info("processing feature ${feature.feature.name}")
                        for (id in feature.entries) {
                            logger.info("processing feature entry $id")
                            val featureEntry = modpack.findEntryById(id)!!
                            val dependencies = getDependencies(modpack, id)
                            logger.info("required dependencies of $id: ${featureEntry.dependencies.filterValues { it == DependencyType.REQUIRED}.keys}")
                            logger.info("optional dependencies of $id: ${featureEntry.dependencies.filterValues { it == DependencyType.OPTIONAL}.keys}")
                            feature.entries += dependencies.asSequence().filter { entry ->
                                logger.debug("  testing ${entry.id}")
                                // find all other entries that depend on this dependency
                                val dependants = modpack.entrySet.filter { otherEntry ->
                                    otherEntry.dependencies.filterValues { it == DependencyType.REQUIRED }.keys.any {
                                        it == entry.id
                                    } ?: false
                                }
                                logger.debug("  dependants to optional of ${entry.id}: ${dependants.associate { it.id to it.optional }}")
                                val allOptionalDependants = dependants.all { filteredEntry -> filteredEntry.optional }
                                entry.optional && !feature.entries.contains(entry.id) && allOptionalDependants
                            }.map { it.id }
                        }
                        logger.info("build entry: ${feature.entries.first()}")
                        val mainEntry = modpack.findEntryById(feature.entries.first())!!
                        feature.feature.description = mainEntry.description ?: ""

                        logger.info("processed feature ${feature.feature.name}")
                    }
                }

//            logger.debug("targetFiles: $targetFiles")

                // write features
                val patterns = "write features".watch {
                    val deferredPatterns = features.map { feature ->
                        async(pool + CoroutineName("properties-${feature.feature.name}")) {
                            logger.info("processing properties: ${feature.feature.name}")
                            for (id in feature.entries) {
                                logger.info(id)
                                logger.info("$id targetfiles: $targetFiles")

                                val targetFile = targetFiles[id]?.let { targetFile ->
                                    targetFile.parentFile.let { parent ->
                                        if (parent.name == "_SERVER" || parent.name == "_CLIENT") {
                                            parent.parentFile.resolve(targetFile.name)
                                        } else
                                            targetFile
                                    }
                                }!!

                                feature.files.include += targetFile.relativeTo(skSrcFolder).path
                                    .replace('\\', '/')
                                    .replace("[", "\\[")
                                    .replace("]", "\\]")
                                logger.info("includes = ${feature.files.include}")
                            }

                            logger.info("entries: ${feature.entries}")
                            logger.info("properties: ${feature.feature}")

                            logger.info("processed properties $feature")

                            FeaturePattern(
                                feature = feature.feature,
                                filePatterns = feature.files
                            )
                        }
                    }

                    delay(10)
                    logger.info("waiting for properties jobs to finish")

                    deferredPatterns.awaitAll()
                }

                val thumb = modpack.packOptions.skCraftOptions.thumb
                    ?: modpack.packOptions.baseUrl?.let { baseUrl ->
                        modpack.iconFile.takeIf { it.exists() }?.let { iconFile ->
                            val targetFile = output.resolve("icon").resolve(iconFile.name)
                            iconFile.copyTo(targetFile, overwrite = true)
                            val relativePath = targetFile.relativeTo(uploadBaseDir).unixPath
                            URI(baseUrl).resolve(relativePath).toASCIIString()
                        }
                    }

                //TODO: figure out icon url, and server url
                val skmodpack = SKModpack(
                    name = modpack.id,
                    title = modpack.title.blankOr ?: "",
                    thumb = thumb,
                    server = modpack.packOptions.skCraftOptions.server,
                    gameVersion = modpack.mcVersion,
                    userFiles = modpack.packOptions.skCraftOptions.userFiles,
                    launch = modpack.launch,
                    features = patterns
                )

                val modpackPath = modpackDir.resolve("modpack.json")
                modpackPath.writeText(json.stringify(SKModpack.serializer(), skmodpack))

                // add to workspace.json
                logger.info("adding ${modpack.id} to workpace.json", modpack.id)
                val workspaceMetaFolder = workspaceDir.resolve(".modpacks")
                workspaceMetaFolder.mkdirs()
                val workspacePath = workspaceMetaFolder.resolve("workspace.json")
                val workspace = if (workspacePath.exists()) {
                    try {
                        json.parse<SKWorkspace>(SKWorkspace.serializer(), workspacePath.readText())
                    } catch (e: Exception) {
                        logger.error("failed parsing: $workspacePath", e)
                        SKWorkspace()
                    }
                } else {
                    SKWorkspace()
                }
                workspace.packs += SKLocation(modpack.id)

                workspacePath.writeText(json.stringify(SKWorkspace.serializer(), workspace))

                val manifestDest = output.resolve("${modpack.id}.json")

                val uniqueVersion = "${modpack.version}." + DateTimeFormatter
                    .ofPattern("yyyyMMddHHmm")
                    .withZone(ZoneOffset.UTC)
                    .format(Instant.now())

                "sk package builder".watch {
                    PackageBuilder.main(
                        "--version", uniqueVersion,
                        "--input", modpackDir.path,
                        "--output", output.path,
                        "--manifest-dest", manifestDest.path,
                        "--pretty-print"
                    )
                }

                // regenerate packages.json
                val packagesFile = output.resolve("packages.json")
                val packages: SKPackages = if (packagesFile.exists()) {
                    json.parse(SKPackages.serializer(), packagesFile.readText())
                } else {
                    SKPackages()
                }

                val packFragment = packages.packages.find { it.name == modpack.id }
                    ?: SkPackageFragment(
                        title = modpack.title.blankOr ?: "",
                        name = modpack.id,
                        version = uniqueVersion,
                        location = "${modpack.id}.json"
                    ).apply { packages.packages += this }
                packFragment.version = uniqueVersion
                packagesFile.writeText(json.stringify(SKPackages.serializer(), packages))

                logger.info("finished")
            }
        }
    }

    // TODO: move to sk specific code
    private fun resolveFeatureDependencies(
        stopwatch: Stopwatch,
        modPack: LockPack,
        entry: LockEntry,
        defaultName: String,
        features: MutableList<ExtendedFeaturePattern>
    ) = stopwatch {
        val entryOptionalData = entry.optionalData ?: return@stopwatch
        val entryFeature = Feature(entry.displayName, entryOptionalData.selected, description = entry.description ?: "")
        val featureName = entry.displayName.takeIf { it.isNotBlank() } ?: defaultName
        // find feature with matching id
        var feature = features.find { f -> f.feature.name == featureName }

        // TODO: merge existing features with matching id
        if (feature == null) {
            var description = entryFeature.description
            if (description.isEmpty()) description = entry.description ?: ""
            feature = ExtendedFeaturePattern(
                entries = setOf(entry.id),
                files = entryFeature.files,
                feature = Feature(
                    name = featureName,
                    selected = entryFeature.selected,
                    description = description,
                    recommendation = entryFeature.recommendation
                )
            )
            processFeature("$featureName-process".watch, modPack, feature)
            features += feature
            entry.optional = true
        }
        logger.debug("processed ${entry.id}")
    }

    /**
     * iterates through all entries in feature
     * and add dependencies also as entries to the feature
     */
    private fun processFeature(
        stopwatch: Stopwatch,
        modPack: LockPack,
        feature: ExtendedFeaturePattern
    ) = stopwatch {
        logger.info("processing feature: $feature")
        val processedEntries = mutableListOf<String>()
        var processableEntries = feature.entries.filter { f -> !processedEntries.contains(f) }
        while (processableEntries.isNotEmpty()) {
            processableEntries = feature.entries.filter { featureName -> !processedEntries.contains(featureName) }
            for (entry_id in processableEntries) {
                logger.info("searching $entry_id")
                val entry = modPack.findEntryById(entry_id)
                if (entry == null) {
                    logger.warn("$entry_id not in entries")
                    processedEntries += entry_id
                    continue
                }
                var depNames = entry.dependencies.keys.toList()
                logger.info("depNames: $depNames")
                depNames = depNames.filter { dependencyId ->
                    modPack.isEntryOptional(dependencyId)
                }
                logger.info("filtered dependency names: $depNames")
                for (dep in depNames) {
                    if (!(feature.entries.contains(dep))) {
                        feature.entries += dep
                    }
                }
                processedEntries += entry_id
            }
        }
    }

    private fun getDependenciesCall(lockPack: LockPack, entryId: String): List<LockEntry> {
        logger.debug("getDependencies of $entryId")
        val entry = lockPack.findEntryById(entryId) ?: return emptyList()
        logger.debug("getting dependencies: ${entry.dependencies}")
        return entry.dependencies.flatMap { (depName, depType) ->
            getDependencies(lockPack, depName)
        }
    }

    private val getDependencies: (lockPack: LockPack, entryName: String) -> List<LockEntry>
        get() {
            return ::getDependenciesCall.memoize()
        }
}
