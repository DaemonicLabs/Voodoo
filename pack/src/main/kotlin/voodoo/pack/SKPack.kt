package voodoo.pack

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
import kotlinx.serialization.json.Json
import voodoo.data.curse.DependencyType
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.forge.ForgeUtil
import voodoo.memoize
import voodoo.pack.sk.SKLocation
import voodoo.pack.sk.SKPackages
import voodoo.pack.sk.SKWorkspace
import voodoo.pack.sk.SkPackageFragment
import voodoo.provider.Providers
import voodoo.util.download
import voodoo.util.pool
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

object SKPack : AbstractPack() {

    override val label = "SK Pack"

    override fun File.getOutputFolder(): File = resolve("sk")

    override suspend fun pack(
        modpack: LockPack,
        output: File,
        clean: Boolean
    ) {
        val cacheDir = directories.cacheHome
        val workspaceDir = modpack.rootDir.resolve("workspace").absoluteFile
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
                if (it.name.endsWith(".entry.hjson") || it.name.endsWith(".lock.hjson") || it.name.endsWith(".lock.pack.hjson"))
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

        coroutineScope {
            // download forge
            modpack.forge?.also { forge ->
                val (forgeUrl, forgeFileName, _, forgeVersion) = ForgeUtil.forgeVersionOf(forge)
                val forgeFile = loadersFolder.resolve(forgeFileName)
                forgeFile.download(forgeUrl, cacheDir.resolve("FORGE").resolve(forgeVersion))
            } ?: logger.warn { "no forge configured" }
            val modsFolder = skSrcFolder.resolve("mods")
            logger.info("cleaning mods $modsFolder")
            modsFolder.deleteRecursively()

            // download entries
            val deferredFiles: List<Deferred<Pair<String, File>>> = modpack.entrySet.map { entry ->
                async(context = pool + CoroutineName("download-${entry.id}")) {
                    val provider = Providers[entry.provider]

                    val targetFolder = skSrcFolder.resolve(entry.serialFile).parentFile

                    val (url, file) = provider.download(entry, targetFolder, cacheDir)
                    if (url != null && entry.useUrlTxt) {
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

            val features = mutableListOf<ExtendedFeaturePattern>()

            for (entry in modpack.entrySet) {
                resolveFeatureDependencies(
                    modpack,
                    entry,
                    modpack.findEntryById(entry.id)?.displayName
                        ?: throw NullPointerException("cannot find lockentry for ${entry.id}"),
                    features
                )
            }

            // resolve features
            for (feature in features) {
                logger.info("processing feature ${feature.feature.name}")
                for (id in feature.entries) {
                    logger.info("processing feature entry $id")
                    val featureEntry = modpack.findEntryById(id)!!
                    val dependencies = getDependencies(modpack, id)
                    logger.info("required dependencies of $id: ${featureEntry.dependencies[DependencyType.REQUIRED]}")
                    logger.info("optional dependencies of $id: ${featureEntry.dependencies[DependencyType.OPTIONAL]}")
                    feature.entries += dependencies.asSequence().filter { entry ->
                        logger.debug("  testing ${entry.id}")
                        // find all other entries that depend on this dependency
                        val dependants = modpack.entrySet.filter { otherEntry ->
                            otherEntry.dependencies[DependencyType.REQUIRED]?.any {
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
                feature.feature.description = mainEntry.description

                logger.info("processed feature ${feature.feature.name}")
            }

            val targetFiles = deferredFiles.awaitAll().toMap()
//            logger.debug("targetFiles: $targetFiles")

            // write features
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

            val patterns = deferredPatterns.awaitAll()

            val skmodpack = SKModpack(
                name = modpack.id,
                title = modpack.title ?: "",
                gameVersion = modpack.mcVersion,
                userFiles = modpack.userFiles,
                launch = modpack.launch,
                features = patterns
            )

            val modpackPath = modpackDir.resolve("modpack.json")
            modpackPath.writeText(Json.indented.stringify(SKModpack.serializer(), skmodpack))

            // add to workspace.json
            logger.info("adding ${modpack.id} to workpace.json", modpack.id)
            val workspaceMetaFolder = workspaceDir.resolve(".modpacks")
            workspaceMetaFolder.mkdirs()
            val workspacePath = workspaceMetaFolder.resolve("workspace.json")
            val workspace = if (workspacePath.exists()) {
                try {
                    Json.indented.parse<SKWorkspace>(SKWorkspace.serializer(), workspacePath.readText())
                } catch (e: Exception) {
                    logger.error("failed parsing: $workspacePath", e)
                    SKWorkspace()
                }
            } else {
                SKWorkspace()
            }
            workspace.packs += SKLocation(modpack.id)

            workspacePath.writeText(Json.indented.stringify(SKWorkspace.serializer(), workspace))

            val manifestDest = output.resolve("${modpack.id}.json")

            val uniqueVersion = "${modpack.version}." + DateTimeFormatter
                .ofPattern("yyyyMMddHHmm")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now())

            PackageBuilder.main(
                "--version", uniqueVersion,
                "--input", modpackDir.path,
                "--output", output.path,
                "--manifest-dest", manifestDest.path,
                "--pretty-print"
            )

            // regenerate packages.json
            val packagesFile = output.resolve("packages.json")
            val packages: SKPackages = if (packagesFile.exists()) {
                Json.indented.parse(SKPackages.serializer(), packagesFile.readText())
            } else {
                SKPackages()
            }

            val packFragment = packages.packages.find { it.name == modpack.id }
                ?: SkPackageFragment(
                    title = modpack.title ?: "",
                    name = modpack.id,
                    version = uniqueVersion,
                    location = "${modpack.id}.json"
                ).apply { packages.packages += this }
            packFragment.version = uniqueVersion
            packagesFile.writeText(Json.indented.stringify(SKPackages.serializer(), packages))

            logger.info("finished")
        }
    }

    // TODO: move to sk specific code
    private fun resolveFeatureDependencies(
        modPack: LockPack,
        entry: LockEntry,
        defaultName: String,
        features: MutableList<ExtendedFeaturePattern>
    ) {
        val entryOptionalData = entry.optionalData ?: return
        val entryFeature = Feature(entry.displayName, entryOptionalData.selected, description = entry.description)
        val featureName = entry.displayName.takeIf { it.isNotBlank() } ?: defaultName
        // find feature with matching id
        var feature = features.find { f -> f.feature.name == featureName }

        // TODO: merge existing features with matching id
        if (feature == null) {
            var description = entryFeature.description
            if (description.isEmpty()) description = entry.description
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
            processFeature(modPack, feature)
            features += feature
            entry.optional = true
        }
        logger.debug("processed ${entry.id}")
    }

    /**
     * iterates through all entries and set
     */
    private fun processFeature(modPack: LockPack, feature: ExtendedFeaturePattern) {
        logger.info("processing feature: $feature")
        val processedEntries = mutableListOf<String>()
        var processableEntries = feature.entries.filter { f -> !processedEntries.contains(f) }
        while (processableEntries.isNotEmpty()) {
            processableEntries = feature.entries.filter { f -> !processedEntries.contains(f) }
            for (entry_id in processableEntries) {
                logger.info("searching $entry_id")
                val entry = modPack.findEntryById(entry_id)
                if (entry == null) {
                    logger.warn("$entry_id not in entries")
                    processedEntries += entry_id
                    continue
                }
                var depNames = entry.dependencies.values.flatten()
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
        val entry = lockPack.findEntryById(entryId) ?: return emptyList()
        val result = mutableListOf(entry)
        for ((depType, entryList) in entry.dependencies) {
            if (depType == DependencyType.EMBEDDED) continue
            for (depName in entryList) {
                result += getDependencies(lockPack, entryId)
            }
        }
        return result
    }

    private val getDependencies: (lockPack: LockPack, entryName: String) -> List<LockEntry>
        get() {
            return ::getDependenciesCall.memoize()
        }
}

