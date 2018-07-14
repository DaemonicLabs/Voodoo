package voodoo.builder

import aballano.kotlinmemoization.memoize
import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonObject
import mu.KotlinLogging
import voodoo.data.curse.DependencyType
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.sk.FeatureProperties
import voodoo.data.sk.SKFeature
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

private fun ModPack.resolveFeatureDependencies(entry: Entry) {
    val modpack = this
    val entryFeature = entry.feature ?: return
    val featureName =/* entryFeature.name.blankOr ?:*/ entry.name
    // find feature with matching name
    var feature = modpack.features.find { f -> f.properties.name == featureName }

    //TODO: merge existing features with matching name
    if (feature == null) {
        var description = entryFeature.description
        if (description.isEmpty()) description = entry.description
        feature = SKFeature(
                entries = setOf(entry.name),
//                processedEntries = emptyList(),
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
//            entry.dependenciesDirty = true
    }
    logger.debug("processed ${entry.name}")
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
fun ModPack.resolve(folder: File, jankson: Jankson, updateAll: Boolean = false, updateDependencies: Boolean = false, updateEntries: List<String>) {
    this.loadEntries(folder, jankson)
    this.loadLockEntry(folder, jankson)


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
            versionsMapping.remove(entry.name)
        }
    }

    if (updateDependencies || updateAll) {
        // remove all transient entries
        versionsMapping.filter { (name, _) ->
            entriesMapping[name]?.first?.transient ?: true
        }.forEach { name, (_, _) ->
            versionsMapping.remove(name)
        }
        //entries = entries.filter { !it.transient }
    }

    fun addEntry(entry: Entry) {
        val filename = entry.name.replace("\\W+".toRegex(), "")
        val file = srcDir.resolve("mods").resolve("$filename.entry.hjson")
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
            logger.info("resolving: ${entry.name}")
            val provider = Provider.valueOf(entry.provider).base

            provider.resolve(entry, this, ::addEntry)?.let { lockEntry ->
                val existingLockEntry = versionsMapping[lockEntry.name]?.first

                if (existingLockEntry == null) {
                    val filename = file.nameWithoutExtension
                    val lockFile = file.absoluteFile.parentFile.resolve("$filename.lock.json")
                    versionsMapping[lockEntry.name] = Pair(lockEntry, lockFile)
                } else {
                    logger.info("existing lockEntry: $existingLockEntry")
                }
                resolved += entry.name
            }
        }
    } while (entriesMapping.any { (name, _) ->
                !resolved.contains(name)
            })

    features.clear()
    entriesMapping.forEach { name, (entry, file, jsonObj) ->
        this.resolveFeatureDependencies(entry)
    }

    // resolve features
    for (feature in features) {
        logger.info("processed feature ${feature.properties.name}")
        for (name in feature.entries) {
            logger.info("processing feature entry $name")
            val dependencies = this.getDependencies(name)
            dependencies
                    .filter {
                        logger.info("testing ${it.name}")
                        it.optional && !feature.entries.contains(it.name)
                    }
                    .forEach {
                        if (!feature.entries.contains(it.name)) {
                            feature.entries += it.name
                            logger.info("includes = ${feature.files.include}")
                        }
                    }
        }
        println { feature.entries.first() }
        val first = entriesMapping.values.firstOrNull { it.first.name == feature.entries.first() }
        println { first }
        val mainEntry = entriesMapping.values.firstOrNull { it.first.name == feature.entries.first() }!!.first
        feature.properties.description = mainEntry.description

        logger.info("processed feature $feature")
    }

//    features.forEach {
//        logger.info("feature: $it")
//    }

    val directories = Directories.get(moduleName = "history")
    val historyPath = directories.dataHome.resolve(this.name)
    historyPath.mkdirs()
    val historyFile = historyPath.resolve("$name-$version.pack.hjson")
    logger.info("adding modpack to history -> $historyFile")
    val historyJson = jankson.toJson(this)
    historyFile.writeText(historyJson.toJson(true, true))
}
