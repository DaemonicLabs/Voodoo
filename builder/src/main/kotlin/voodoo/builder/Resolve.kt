package voodoo.builder

import aballano.kotlinmemoization.memoize
import mu.KotlinLogging
import voodoo.data.Feature
import voodoo.data.curse.DependencyType
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.sk.SKFeature
import voodoo.forge.Forge.getForgeBuild
import voodoo.provider.Provider
import voodoo.util.Directories
import voodoo.util.blankOr
import voodoo.util.writeJson
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

private val logger = KotlinLogging.logger {}

fun getDependenciesCall(entryName: String, modpack: ModPack): List<Entry> {
    val entry = modpack.entries.find { it.name == entryName } ?: return emptyList()
    var result = listOf(entry)
    for ((depType, entryList) in entry.dependencies) {
        if (depType == DependencyType.EMBEDDED) continue
        for (depName in entryList) {
            result += getDependencies(depName, modpack)
        }
    }
    return result
}

val getDependencies = ::getDependenciesCall.memoize()

fun resolveFeatureDependencies(entry: Entry, modpack: ModPack) {
    val entryFeature = entry.feature ?: return
    val featureName = entryFeature.name.blankOr ?: entry.name
    // find feature with matching name
    var feature = modpack.features.find { f -> f.properties.name == featureName }

    if (feature == null) {
        var description = entryFeature.description
        if (description.isEmpty()) description = entry.description
        feature = Feature(
                entries = setOf(entry.name),
//                processedEntries = emptyList(),
                files = entryFeature.files,
                properties = SKFeature(
                        name = featureName,
                        selected = entryFeature.selected,
                        description = description,
                        recommendation = entryFeature.recommendation
                )
        )
        processFeature(feature, modpack)
        modpack.features += feature
        entry.optional = true
//            entry.dependenciesDirty = true
    }
    logger.debug("processed ${entry.name}")
}

private fun processFeature(feature: Feature, modpack: ModPack) {
    logger.info("processing feature: $feature")
    var processedEntries = emptyList<String>()
    var processableEntries = feature.entries.filter { f -> !processedEntries.contains(f) }
    while (processableEntries.isNotEmpty()) {
        processableEntries = feature.entries.filter { f -> !processedEntries.contains(f) }
        for (entry_name in processableEntries) {
            logger.info("searching $entry_name")
            val entry = modpack.entries.find { e ->
                e.name == entry_name
            }
            if (entry == null) {
                logger.warn("$entry_name not in entries")
                processedEntries += entry_name
                continue
            }
            var depNames = entry.dependencies.values.flatten()
            logger.info("depNames: $depNames")
            depNames = depNames.filter { d ->
                modpack.entries.any { e -> e.name == d && e.optional }
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

fun ModPack.resolve(updateAll: Boolean = false, updateEntries: List<String>) {
    if (updateAll) {
        versions.clear()
    } else {
        for (entryName in updateEntries) {
            val entry = this.entries.find { it.name == entryName }
            if (entry == null) {
                logger.error("entry $entryName not found")
                exitProcess(-1)
            }
            versions.remove(entry.name)
        }
    }

    writeVersionCache()

    if(forgeBuild < 0) {
        forgeBuild = getForgeBuild(forge, mcVersion)
    }

    val tmpEntries = mutableListOf<Entry>()

    fun addEntry(entry: Entry) {
        if(entry.name.isBlank()) {
            logger.error ("invalid: $entry" )
        }
        val duplicate = tmpEntries.find { it.name == entry.name }
        if (duplicate == null) {
            entry.transient = true
            tmpEntries += entry
        } else {
            duplicate.side += entry.side
            if(duplicate.feature == null) {
                duplicate.feature = entry.feature
            }
            if(duplicate.description.isBlank()) {
                duplicate.feature = entry.feature
            }
        }
    }

    entries.forEach {
        addEntry(it)
    }

    // remove all transient entries
    entries = entries.filter { !it.transient }
    // recalculate all dependencies
    for (entry in entries) {
        val provider = Provider.valueOf(entry.provider).base
        val lockEntry = provider.resolve(entry, this, ::addEntry)
        if (entry.name.isBlank()) {
            logger.error("entry has no name set: $entry")
            exitProcess(2)
        }
        if (!versions.containsKey(entry.name) && lockEntry != null)
            versions[entry.name] = lockEntry
    }

    this.entries = tmpEntries.toList()

    features.clear()
    for (entry in entries) {
        resolveFeatureDependencies(entry, this)
    }

    // resolve features
    for (feature in features) {
        logger.info("processed feature ${feature.properties.name}")
        for (name in feature.entries) {
            logger.info("processing feature entry $name")
            val dependencies = getDependencies(name, this)
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
        val mainEntry = entries.find { it.name == feature.entries.first() }!!
        feature.properties.description = mainEntry.description

        logger.info("processed feature $feature")
    }
    writeFeatureCache()

    var unresolved: List<Entry> = emptyList()
    while (this.entries.filter { !versions.containsKey(it.name) }.apply { unresolved = this }.isNotEmpty()) {
        for (entry in unresolved) {
            val provider = Provider.valueOf(entry.provider).base

            val lockEntry = provider.resolve(entry, this, ::addEntry)
            if (lockEntry != null)
                versions[entry.name] = lockEntry
        }
    }
    writeVersionCache()

    val directories = Directories.get(moduleName = "history")
    val historyPath = directories.dataHome.resolve(this.name)
    historyPath.mkdirs()
    val historyFile = historyPath.resolve("$name-$version.json")
    logger.info("adding modpack to history -> $historyFile")
    historyFile.writeJson(this)
}
