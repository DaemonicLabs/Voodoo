package voodoo.builder

import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonObject
import mu.KotlinLogging
import voodoo.Builder
import voodoo.data.curse.DependencyType
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.flat.findByid
import voodoo.data.flat.set
import voodoo.data.sk.FeatureProperties
import voodoo.data.sk.SKFeature
import voodoo.memoize
import voodoo.provider.Provider
import java.io.File
import java.lang.IllegalStateException
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

private val logger = KotlinLogging.logger {}

private fun ModPack.getDependenciesCall(entryId: String): List<Entry> {
    val modpack = this
    val entry = modpack.entriesSet.findByid(entryId) ?: return emptyList()
    var result = listOf(entry)
    for ((depType, entryList) in entry.dependencies) {
        if (depType == DependencyType.EMBEDDED) continue
        for (depName in entryList) {
            result += this.getDependencies(depName)
        }
    }
    return result
}

private val ModPack.getDependencies: (entryName: String) -> List<Entry>
    get() = ::getDependenciesCall.memoize()

private fun ModPack.resolveFeatureDependencies(entry: Entry, defaultName: String) {
    val modpack = this
    val entryFeature = entry.feature ?: return
    val featureName = entry.name.takeIf { it.isNotBlank() } ?: defaultName
    // find feature with matching id
    var feature = modpack.features.find { f -> f.properties.name == featureName }

    //TODO: merge existing features with matching id
    if (feature == null) {
        var description = entryFeature.description
        if (description.isEmpty()) description = entry.description
        feature = SKFeature(
                entries = setOf(entry.id),
                files = entryFeature.files,
                properties = FeatureProperties(
                        name = featureName,
                        selected = entryFeature.selected,
                        description = description,
                        recommendation = entryFeature.recommendation
                )
        )
        modpack.processFeature(feature)
        modpack.features += feature
        entry.optional = true
    }
    logger.debug("processed ${entry.id}")
}

private fun ModPack.processFeature(feature: SKFeature) {
    logger.info("processing feature: $feature")
    var processedEntries = emptyList<String>()
    var processableEntries = feature.entries.filter { f -> !processedEntries.contains(f) }
    while (processableEntries.isNotEmpty()) {
        processableEntries = feature.entries.filter { f -> !processedEntries.contains(f) }
        for (entry_id in processableEntries) {
            logger.info("searching $entry_id")
            val entry = entriesSet.findByid(entry_id)
            if (entry == null) {
                logger.warn("$entry_id not in entries")
                processedEntries += entry_id
                continue
            }
            var depNames = entry.dependencies.values.flatten()
            logger.info("depNames: $depNames")
            depNames = depNames.filter { d ->
                entriesSet.any { entry -> entry.id == d && entry.optional }
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

/**
 * ensure entries are loaded before calling resolve
 */
suspend fun ModPack.resolve(folder: File, jankson: Jankson, updateAll: Boolean = false, updateDependencies: Boolean = false, updateEntries: List<String> = listOf()) {
    this.loadEntries(folder, jankson)
    this.loadLockEntries(folder, jankson)

    val srcDir = folder.resolve(sourceDir)

    if (updateAll) {
        lockEntrySet.clear()
//        versions.clear()
    } else {
        for (entryId in updateEntries) {
            val entry = entriesSet.findByid(entryId)
            if (entry == null) {
                logger.error("entry $entryId not found")
                exitProcess(-1)
            }
            lockEntrySet.removeIf { it.id == entry.id }
        }
    }

    if (updateDependencies || updateAll) {
        // remove all transient entries
        lockEntrySet.removeIf { (id, _) ->
            entriesSet.findByid(id)?.transient ?: true
        }
    }

    fun addEntry(entry: Entry, path: String = "mods") {
        val filename = entry.id.replace("[^\\w-]+".toRegex(), "")
        val file = srcDir.resolve(path).resolve("$filename.entry.hjson")
        this.addEntry(entry, file, dependency = true)
    }

    // recalculate all dependencies
    val resolved: MutableSet<String> = mutableSetOf()
    do {
        val unresolved: List<Entry> = entriesSet.filter { entry ->
            !resolved.contains(entry.id)
        }
        logger.info("unresolved: ${unresolved.map { it.id }}")
        logger.info("resolved: $resolved")
        unresolved.forEach { entry ->
            logger.info("resolving: ${entry.id}")
            val provider = Provider.valueOf(entry.provider).base

            provider.resolve(entry, this.mcVersion, ::addEntry).let { lockEntry ->
                val existingLockEntry = lockEntrySet.findByid(lockEntry.id)

                if (!provider.validate(lockEntry)) {
                    Builder.logger.error { lockEntry }
                    throw IllegalStateException("entry did not validate")
                }

                val actualLockEntry = if (existingLockEntry == null) {
                    val filename = entry.file.nameWithoutExtension
                    lockEntry.file = entry.file.absoluteFile.parentFile.resolve("$filename.lock.json")
                    lockEntrySet[lockEntry.id] = lockEntry
                    lockEntry
                } else {
                    logger.info("existing lockEntry: $existingLockEntry")
                    existingLockEntry
                }
                if (!provider.validate(actualLockEntry)) {
                    Builder.logger.error { actualLockEntry }
                    throw IllegalStateException("actual entry did not validate")
                }

                actualLockEntry.name = actualLockEntry.name()

                resolved += entry.id
            }
        }
    } while (unresolved.isNotEmpty())

    features.clear()

    for (entry in entriesSet) {
        this.resolveFeatureDependencies(entry, lockEntrySet.findByid(entry.id)!!.name())
    }
//    entriesSet.forEach { id, (entry, file, jsonObj) ->
//        this.resolveFeatureDependencies(entry)
//    }

    // resolve features
    for (feature in features) {
        logger.info("processed feature ${feature.properties.name}")
        for (id in feature.entries) {
            logger.info("processing feature entry $id")
            val dependencies = this.getDependencies(id)
            dependencies
                    .filter {
                        logger.info("testing ${it.id}")
                        it.optional && !feature.entries.contains(it.id)
                    }
                    .forEach {
                        if (!feature.entries.contains(it.id)) {
                            feature.entries += it.id
                            logger.info("includes = ${feature.files.include}")
                        }
                    }
        }
        println { feature.entries.first() }
        val mainEntry = entriesSet.findByid(feature.entries.first())!!
        feature.properties.description = mainEntry.description

        logger.info("processed feature $feature")
    }

//    features.forEach {
//        logger.info("feature: $it")
//    }

    //TODO: rethink history, since packs are now mainly file based
//    val directories = Directories.get()
//    val historyPath = directories.dataHome.resolve(this.id)
//    historyPath.mkdirs()
//    val historyFile = historyPath.resolve("$id-$version.pack.hjson")
//    logger.info("adding modpack to history -> $historyFile")
//    val historyJson = jankson.toJson(this)
//    historyFile.writeText(historyJson.toJson(true, true))
}
