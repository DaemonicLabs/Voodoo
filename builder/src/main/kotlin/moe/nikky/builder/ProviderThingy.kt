package moe.nikky.builder

import aballano.kotlinmemoization.memoize
import aballano.kotlinmemoization.tuples.Quintuple
import moe.nikky.builder.provider.*
import mu.KLogging
import java.io.File

/**
 * Created by nikky on 04/01/18.
 * @author Nikky
 * @version 1.0
 */

enum class Provider(val thingy: ProviderThingy) {
    CURSE(CurseProviderThing()),
    DIRECT(DirectProviderThing()),
    LOCAL(LocalProviderThing()),
    JENKINS(JenkinsProviderThing()),
    DUMMY(DummyProviderThing())
}

private var processedFeatures = listOf<String>() //TODO: move into modpack-shared object
private val processedFunctions = mutableMapOf<String, List<String>>() //TODO: move into modpack-shared object

abstract class ProviderThingy {
    open val name = "abstract Provider"
    private var functions = listOf<Quintuple<String, (Entry) -> Boolean, (Entry, Modpack) -> Unit, Boolean, String>>()
    //    private var functions = mutableMapOf<Provider, List<Triple<String, (Entry) -> Boolean, (Entry, Modpack) -> Unit>>>()

    companion object : KLogging()

    init {
        register("setFeatureName",
                { !it.feature?.name.isNullOrBlank() },
                { e, _ ->
                    e.feature!!.name = e.name
                }
        )

        register("postResolveDependencies",
                { it.name.isNotBlank() && ((it.provider == Provider.CURSE) == it.resolvedDependencies) },
                { e, m ->
                    logger.info("run after resolveDependencies ${e.name}")
                }
        )

        register("resolveProvides",
                { it.name.isNotBlank() },
                { e, m ->
                    logger.info("provides starting: $e")
                    m.mods.entries.forEach { entry ->
                        entry.dependencies.forEach { type, dependencies ->
                            var provides = e.provides[type] ?: listOf()
                            dependencies.forEach { dependency ->
                                if (e.name == dependency && !provides.contains(entry.name)) {
                                    logger.info("${e.name} provides $type ${entry.name}")
                                    provides += entry.name
                                }
                            }
                            e.provides[type] = provides
                        }
                    }
                    logger.info("provides finished: $e")
                },
                requires = "postResolveDependencies"
        )
        register("resolveFeatureDependencies",
                { it.name.isNotBlank() && !processedFeatures.contains(it.name) },
                ::resolveFeatureDependencies,
                repeatable = true,
                requires = "resolveProvides"
        )
//        register("cleanDependneciy",
//                { it.dependenciesDirty },
//                { e, m ->
//                    val required = e.dependencies[DependencyType.required] ?: emptyList()
//                    for (name in required) {
//                        val depEntry = m.entries.firstOrNull { it.name == name }!!
//                        depEntry.optional = e.optional && depEntry.optional
//                        depEntry.dependenciesDirty = true
//                    }
//                    e.dependenciesDirty = false
//                },
//                true
//        )
//        register("checkCleanDependency",
//                { !it.resolvedOptionals && !it.dependenciesDirty },
//                { e, m ->
//                    val required = e.dependencies[DependencyType.required] ?: emptyList()
//                    for (name in required) {
//                        val depEntry = m.entries.firstOrNull { it.name == name }!!
//                        depEntry.optional = e.optional && depEntry.optional
//                        depEntry.dependenciesDirty = true
//                    }
//                    e.resolvedOptionals = true
//                },
//                true
//        )
        register("resolveOptional",
                { !it.resolvedOptionals },
                { e, m ->
                    e.optional = isOptional(e, m)
                    e.resolvedOptionals = true
                },
                repeatable = true,
                requires = "resolveFeatureDependencies"
        )

        register("setCachePath",
                { it.cachePath.isBlank() && it.cacheRelpath.isNotBlank() },
                { e, m ->
                    e.cachePath = File(m.cacheBase).resolve(e.cacheRelpath).path
                }
        )

        register("resolvePath",
                { it.filePath.isBlank() && it.targetPath.isNotBlank() && it.fileName.isNotBlank() },
                { e, _ ->
                    var path = e.targetPath
                    // side
                    if (path.startsWith("mods")) {
                        val side = when (e.side) {
                            Side.CLIENT -> "_CLIENT"
                            Side.SERVER -> "_SERVER"
                            Side.BOTH -> ""
                        }
                        if (side.isNotBlank()) {
                            path = "$path/$side"
                        }
                    }
                    e.path = path
                    e.filePath = File(e.basePath, e.path).resolve(e.fileName).path
                }
        )
        register("resolveTargetFilePath",
                { it.targetFilePath.isBlank() && it.targetPath.isNotBlank() && it.fileName.isNotBlank() },
                { e, _ ->
                    e.targetFilePath = File(e.targetPath).resolve(e.fileName).path
                }
        )
    }

    fun register(label: String, condition: (Entry) -> Boolean, execute: (Entry, Modpack) -> Unit, repeatable: Boolean = false, requires: String = "", force: Boolean = false) {
        val duplicate = functions.find { it.first == label }
        if (duplicate != null) {
            if (force) {
                functions -= duplicate
            } else {
                logger.warn("cannot register duplicate $label")
                return
            }
        }
        logger.info("registering $label to $this")
        functions += Quintuple(label, condition, execute, repeatable, requires)
    }

    fun process(entry: Entry, modpack: Modpack): Boolean {
        var processed = processedFunctions.getOrDefault(entry.name, emptyList())
        for ((label, condition, execute, repeatable, requirement) in functions) {
            if (!repeatable && processed.contains(label)) {
                continue
            }
            if (requirement.isNotBlank()) {
//                var temp = processedFunctions[entry.name] ?: listOf()
//                println("processed functions of ${entry.name} $temp")
                val fulfilled = modpack.mods.entries.all {
                    processedFunctions.getOrDefault(it.name, emptyList()).contains(requirement)
                }
                if (!fulfilled) {
                    logger.debug("processed map: $processedFunctions")
                    val missing = modpack.mods.entries.filter {
                        !processedFunctions.getOrDefault(it.name, emptyList()).contains(requirement)
                    }.map { if (it.name.isNotBlank()) it.name else it.toString() }
                    logger.warn("requirement $requirement is not fulfilled by all entries, missing: $missing")
                    continue
                }
            }
            if (condition(entry)) {
                logger.debug("executing $label")
                //TODO: check if process failed (no change to entry or modpack)
                execute(entry, modpack)
                processed += label
                processedFunctions[entry.name] = processed
                return true
            }
        }
        logger.warn("no action matched for entry $entry")
        //TODO: keep count if times a entry has fallen through consecutively, kill it after > X time
        return false
    }

    private fun isOptionalCall(entry: Entry, modpack: Modpack): Boolean {
        logger.info("test optional of ${entry.name}")
//        logger.info(entry.toString())
        var result = entry.transient || entry.optional
        if (result) return result
        for ((depType, entryList) in entry.provides) {
            if (depType != DependencyType.required) continue
            if (entryList.isEmpty()) return false
            logger.info("type: $depType list: $entryList")
            for (entryName in entryList) {
                val providerEntry = modpack.mods.entries.firstOrNull { it.name == entryName }!!
                val tmpResult = isOptional(providerEntry, modpack)
                if (!tmpResult) return false
            }
        }
        return true
    }

    private fun isOptionalCallBackup(entry: Entry, modpack: Modpack): Boolean {
        var result = entry.transient || entry.optional
        for ((depType, entryList) in entry.provides) {
            if (depType != DependencyType.required) continue
            for (entryName in entryList) {
                val providerEntry = modpack.mods.entries.firstOrNull { it.name == entryName }!!
                result = result && isOptional(providerEntry, modpack)
                if (!result) return result
            }
        }
        return result
    }

    val isOptional = ::isOptionalCall.memoize()

    fun checkCleanDependency(entry: Entry, modpack: Modpack): Boolean {
        if (!entry.resolvedOptionals) return false
        for ((depType, depList) in entry.dependencies) {
            for (depNames in depList) {
                val depEntry = modpack.mods.entries.firstOrNull { it.name == name }!!
                if (!checkCleanDependency(depEntry, modpack)) {
                    return false
                }
            }
        }
        return true
    }

    fun resolveFeatureDependencies(entry: Entry, modpack: Modpack) {
        if (entry.feature == null) {
            processedFeatures += entry.name
            return
        }
        val entryFeature = entry.feature ?: return
        var featureName = entryFeature.name
        if (featureName.isBlank())
            featureName = entry.name
        // find feature with matching name
        var feature = modpack.features.find { f -> f.properties.name == featureName }

        if (feature == null) {
            feature = Feature(
                    names = listOf(featureName),
                    entries = listOf(entry.name),
                    processedEntries = emptyList(),
                    files = entryFeature.files,
                    properties = SKFeatureProperties(
                            name = featureName,
                            selected = entryFeature.selected,
                            description = entryFeature.description,
                            recommendation = entryFeature.recommendation
                    )
            )
            processFeature(feature, modpack)
            modpack.features += feature
            entry.optional = true
//            entry.dependenciesDirty = true
        }
        processedFeatures += entry.name
        logger.debug("processed ${entry.name} -> $processedFeatures")
    }

    private fun processFeature(feature: Feature, modpack: Modpack) {
        logger.info("processing feature: $feature")
        var processedEntries = emptyList<String>()
        var processableEntries = feature.entries.filter { f -> !processedEntries.contains(f) }
        while (processableEntries.isNotEmpty()) {
            processableEntries = feature.entries.filter { f -> !processedEntries.contains(f) }
            for (entry_name in processableEntries) {
                logger.info("searching $entry_name")
                val entry = modpack.mods.entries.find { e ->
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
                    modpack.mods.entries.any { e -> e.name == d }
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
}