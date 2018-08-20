package voodoo.builder

import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonObject
import mu.KotlinLogging
import voodoo.data.curse.DependencyType
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.sk.FeatureProperties
import voodoo.data.sk.SKFeature
import voodoo.memoize
import voodoo.provider.Provider
import voodoo.util.Directories
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

private val logger = KotlinLogging.logger {}

private fun ModPack.getDependenciesCall(entryName: String): List<Entry> {
    val modpack = this
    val entry = modpack.entriesMapping[entryName]?.first ?: return emptyList()
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
        for (entry_name in processableEntries) {
            logger.info("searching $entry_name")
            val entry = entriesMapping[entry_name]?.first
            if (entry == null) {
                logger.warn("$entry_name not in entries")
                processedEntries += entry_name
                continue
            }
            var depNames = entry.dependencies.values.flatten()
            logger.info("depNames: $depNames")
            depNames = depNames.filter { d ->
                entriesMapping.any { (name, triple) -> name == d && triple.first.optional }
            }
            logger.info("filtered dependency names: $depNames")
            for (dep in depNames) {
                if (!(feature.entries.contains(dep))) {
                    feature.entries += dep
                }
            }
            processedEntries += entry_name
        }
    }
}

/**
 * ensure entries are loaded before calling resolve
 */
suspend fun ModPack.resolve(folder: File, jankson: Jankson, updateAll: Boolean = false, updateDependencies: Boolean = false, updateEntries: List<String>) {
    this.loadEntries(folder, jankson)
    this.loadLockEntries(folder, jankson)

    val srcDir = folder.resolve(sourceDir)

    if (updateAll) {
        versionsMapping.clear()
//        versions.clear()
    } else {
        for (entryName in updateEntries) {
            val entry = entriesMapping[entryName]?.first
            if (entry == null) {
                logger.error("entry $entryName not found")
                exitProcess(-1)
            }
            versionsMapping.remove(entry.id)
        }
    }

    if (updateDependencies || updateAll) {
        // remove all transient entries
        versionsMapping.filter { (id, _) ->
            entriesMapping[id]?.first?.transient ?: true
        }.forEach { name, (_, _) ->
            versionsMapping.remove(name)
        }
    }

    fun addEntry(entry: Entry, path: String = "mods") {
        val filename = entry.id.replace("[^\\w-]+".toRegex(), "")
        val file = srcDir.resolve(path).resolve("$filename.entry.hjson")
        val jsonObj = jankson.toJson(entry) as JsonObject
        this.addEntry(entry, file, jsonObj, jankson, dependency = true)
    }

    // recalculate all dependencies
    val resolved: MutableSet<String> = mutableSetOf()
    do {
        val unresolved: List<Triple<Entry, File, JsonObject>> = entriesMapping.filter { (name, _) ->
            !resolved.contains(name)
        }.map { it.value }
        logger.info("resolved: $resolved")
        unresolved.forEach { (entry, file, _) ->
            logger.info("resolving: ${entry.id}")
            val provider = Provider.valueOf(entry.provider).base

            provider.resolve(entry, this, ::addEntry)?.let { lockEntry ->
                val existingLockEntry = versionsMapping[lockEntry.id]?.first

                val actualLockEntry = if (existingLockEntry == null) {
                    val filename = file.nameWithoutExtension
                    val lockFile = file.absoluteFile.parentFile.resolve("$filename.lock.json")
                    versionsMapping[lockEntry.id] = Pair(lockEntry, lockFile)
                    lockEntry
                } else {
                    logger.info("existing lockEntry: $existingLockEntry")
                    existingLockEntry
                }

                actualLockEntry.name = actualLockEntry.name()

                resolved += entry.id
            }
        }
    } while (entriesMapping.any { (name, _) ->
                !resolved.contains(name)
            })

    features.clear()

    for ((name, triple) in entriesMapping) {
        val (entry, file, jsonObj) = triple
        this.resolveFeatureDependencies(entry, versionsMapping[entry.id]!!.first.name())
    }
//    entriesMapping.forEach { id, (entry, file, jsonObj) ->
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
        val first = entriesMapping.values.firstOrNull { it.first.id == feature.entries.first() }
        println { first }
        val mainEntry = entriesMapping.values.firstOrNull { it.first.id == feature.entries.first() }!!.first
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
