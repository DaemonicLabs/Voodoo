package moe.nikky.builder

import moe.nikky.builder.provider.CurseProviderThingy
import moe.nikky.builder.provider.DirectProviderThing
import java.io.File

/**
 * Created by nikky on 04/01/18.
 * @author Nikky
 * @version 1.0
 */

enum class Provider(val thingy: ProviderThingy) {
    CURSE(CurseProviderThingy()),
    DIRECT(DirectProviderThing())
}

abstract class ProviderThingy {
    open val name = "abstract Provider"
    private var functions = listOf<Triple<String, (Entry) -> Boolean, (Entry, Modpack) -> Unit>>()
    //    private var functions = mutableMapOf<Provider, List<Triple<String, (Entry) -> Boolean, (Entry, Modpack) -> Unit>>>()
    private val processedFunctions = mutableMapOf<Entry, List<String>>()
    private var processedFeatures = listOf<String>()

    companion object {
//        private var functions = listOf<Triple<String, (Entry) -> Boolean, (Entry, Modpack) -> Unit>>()
    }

    init {
        register("setFeatureName",
                { !it.feature?.name.isNullOrBlank() },
                { e, _ ->
                    e.feature!!.name = e.name
                }
        )
        register("resolveFeatureDependencies",
                { it.name.isNotBlank() && it.feature != null && !processedFeatures.contains(it.name) },
                ::resolveFeatureDependencies
        )

        register("setCachePath",
                { it.cachePath.isBlank() && it.cacheRelpath.isNotBlank()},
                { e, m ->
                    e.cachePath = File(m.cacheBase).resolve(e.cacheRelpath).path
                }
        )

        register("resolvePath",
                { it.path.isNotBlank() /*|| it.basePath == "loaders"*/ },
                { e, _ ->
                    var path = e.path
                    // side
                    if (path.startsWith("mods")) {
                        val side = when (e.side) {
                            Side.CLIENT -> "_CLIENT"
                            Side.SERVER -> "_SERVER"
                            Side.BOTH -> ""
                        }
                        if (side.isNotBlank()) {
                            path = "$side/$path"
                        }
                    }
                    e.path = path
                    e.filePath = File(e.basePath, e.path).resolve(e.fileName).path
                }
        )
    }

    fun register(label: String, condition: (Entry) -> Boolean, execute: (Entry, Modpack) -> Unit) {
        if (functions.find { it.first == label } != null) {
            println("cannot register duplicate $label")
            return
        }
        println("registering ${this} $label")
        functions += Triple(label, condition, execute)
    }

    fun process(entry: Entry, modpack: Modpack): Boolean {
        val processed = processedFunctions.getOrDefault(entry, emptyList())
        for ((label, condition, execute) in functions) {
            if (processed.contains(label)) continue
            if (condition(entry)) {
                println("executing $label")
                //TODO: check if process failed (no change to entry or modpack)
                execute(entry, modpack)
                processedFunctions[entry] = processed + label
                return true
            }
        }
        println("no action matched for entry $entry")
        return false
    }

    fun resolveFeatureDependencies(entry: Entry, modpack: Modpack) {
        var featureName = entry.feature?.name ?: return
        if (featureName.isBlank())
            featureName = entry.name
        // find feature with matching name
        var feature = modpack.features.find { f -> f.name == featureName }

        if (feature == null) {
            println(entry)
            feature = Feature(
                    name = featureName,
                    names = listOf(featureName),
                    entries = listOf(entry.name),
                    processedEntries = emptyList()
            )
            processFeature(feature, modpack)
            modpack.features += feature
        }
        processedFeatures += entry.name
    }

    private fun processFeature(feature: Feature, parent: Modpack) {
        println("processing $feature")
        val processableEntries = feature.entries.filter { f -> !feature.processedEntries.contains(f) }
        for (entry_name in processableEntries) {
            println("searching $entry_name")
            val entry = parent.entries.find { e ->
                e.name == entry_name
            }
            if (entry == null) {
                println("$entry_name not in entries")
                feature.processedEntries += entry_name
                continue
            }
            var depNames = entry.dependencies.values.flatten()
            print(depNames)
            depNames = depNames.filter { d ->
                parent.entries.any { e -> e.name == d }
            }
            println("filtered dependency names: $depNames")
            for (dep in depNames) {
                if (!(feature.entries.contains(dep))) {
                    feature.entries += dep
                }
            }
            feature.processedEntries += entry_name
        }
    }


//    abstract fun validate(): Boolean
//    open fun prepareDependencies(modpack: Modpack) {
//        println("prepareDependencies not overridden in '$name'")
//    }
//
//    open fun resolveDependencies(modpack: Modpack) {
//        println("resolveDependencies not overridden in '$name'")
//    }
//
//    open fun resolveFeatureDependencies(modpack: Modpack) {
//        var featureName = entry.feature?.name ?: return
//        if (featureName.isBlank())
//            featureName = entry.name
//        // find feature with matching name
//        var feature = modpack.features.find { f -> f.name == featureName }
//
//        if (feature == null) {
//            println(entry)
//            feature = Feature(
//                    name = featureName,
//                    names = listOf(featureName),
//                    entries = listOf(entry.name),
//                    processedEntries = emptyList()
//            )
//            processFeature(feature, modpack)
//            modpack.features += feature
//        }
//    }
//
//    private fun processFeature(feature: Feature, parent: Modpack) {
//        val features = parent.features
//        println("processing $feature")
//        val processableEntries = feature.entries.filter { f -> !feature.processedEntries.contains(f) }
//        for (entry_name in processableEntries) {
//            println("searching $entry_name")
//            val entry = parent.entries.find { e ->
//                e.name == entry_name
//            }
//            if (entry == null) {
//                println("$entry_name not in entries")
//                feature.processedEntries += entry_name
//                continue
//            }
//            var depNames = entry.dependencies.values.flatten()
//            print(depNames)
//            depNames = depNames.filter { d ->
//                parent.entries.any { e -> e.name == d }
//            }
//            println("filtered dependency names: $depNames")
//            for (dep in depNames) {
//                if (!(feature.entries.contains(dep))) {
//                    feature.entries += dep
//                }
//            }
//            feature.processedEntries += entry_name
//        }
//    }
//
//    open fun fillInformation() {
//        if (entry.feature != null) {
//            if (entry.feature!!.name.isBlank()) {
//                entry.feature!!.name = entry.name
//            }
//        }
//    }
//
//    abstract fun prepareDownload(cacheBase: File)
//
//    fun resolvePath() {
//
//        var path = entry.path
//        // side
//        if(path.startsWith("mods")) {
//            val side = when(entry.side) {
//                Side.CLIENT -> "_CLIENT"
//                Side.SERVER -> "_SERVER"
//                Side.BOTH -> ""
//            }
//            if(side.isNotBlank()) {
//                path = "$side/$path"
//            }
//        }
//        entry.path = path
//        entry.filePath = File(entry.basePath, entry.path).resolve(entry.fileName).path
//    }

//    fun writeUrlTxt(outputPath: File) {
//        if(entry.url.isBlank()) throw Exception("entry $entry misses url")
//        if(entry.filePath.isBlank()) throw Exception("entry $entry misses filePath")
//        val urlPath = File(outputPath, entry.filePath + ".url.txt")
//        File(urlPath.parent).mkdirs()
//        urlPath.writeText(URLDecoder.decode(entry.url, "UTF-8"))
//    }

//    abstract fun download(outputPath: File)
}