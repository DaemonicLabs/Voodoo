package voodoo.builder

import mu.KLogging
import voodoo.builder.data.Entry
import voodoo.builder.data.Feature
import voodoo.builder.data.Modpack
import voodoo.builder.data.SKFeatureProperties

data class Job(
        @Deprecated("label should be passed to JobManager")
        val label: String,
        val condition: (Entry, Modpack) -> Boolean,
        val execute: (Entry, Modpack) -> Unit,
        val repeatable: Boolean = false,
        val requires: String = ""
) {

    //maybe cache this to avoid searching again and again
//    private val entry: Entry?
//        get() = modpack.mods.entries.find { it.name == entryname }
}

class JobTracker(val modpack: Modpack) {
    var processedFeatures = listOf<String>()
    val processedFunctions = mutableMapOf<String, List<String>>()

    init {
        logger.info("creating JobTracker for ${modpack.name}")
    }

    companion object : KLogging()/* {
        val instance = {modpack: Modpack -> JobTracker(modpack)}.memoize()
    }*/
}

class JobManager(val type: String) {
    var jobs = hashMapOf<String, Job>() // all the registered jobs
        private set

    companion object : KLogging() {
        private val requirementWarning = mutableMapOf<String, Boolean>()

        fun resetWarnings() {
            requirementWarning.clear()
        }
    }

    fun register(label: String, job: Job) {
        jobs[label] = job
    }

    fun process(entry: Entry, modpack: Modpack): Boolean {
        val tracker = modpack.tracker

        var processed = tracker.processedFunctions.getOrDefault(entry.name, emptyList())
        var matched = false
        for ((label, job) in jobs) {
            if (!job.repeatable && processed.contains(label)) {
                continue
            }
            if (job.requires.isNotBlank()) {
                val fulfilled = modpack.mods.entries.all {
                    tracker.processedFunctions.getOrDefault(it.name, emptyList()).contains(job.requires)
                }
                if (!fulfilled) {
                    logger.debug("processed map: ${tracker.processedFunctions}")
                    val missing = modpack.mods.entries.filter {
                        !tracker.processedFunctions.getOrDefault(it.name, emptyList()).contains(job.requires)
                    }.map { if (it.name.isNotBlank()) it.name else it.toString() }
                    if (requirementWarning[job.requires] != true) {
                        logger.warn("requirement ${job.requires} is not fulfilled by all entries, missing: $missing")
                        requirementWarning[job.requires] = true
                    }
                    continue
                }
            }
            if (job.condition(entry, modpack)) {
                logger.debug("executing $label")
                //TODO: check if process failed (no change to entry or modpack)
                job.execute(entry, modpack)
                processed += label
                tracker.processedFunctions[entry.name] = processed
                logger.debug("${entry.name} matched job $label")
                matched = true
                continue
            }
        }
        if (!matched) {
            logger.warn("no action matched for entry $entry")
            //TODO: keep count if times a entry has fallen through consecutively, kill it after > X time
        }
        return matched
    }

    fun resolveFeatureDependencies(entry: Entry, modpack: Modpack) {
        val tracker = modpack.tracker
        if (entry.feature == null) {
            tracker.processedFeatures += entry.name
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
        tracker.processedFeatures += entry.name
        logger.debug("processed ${entry.name} -> ${tracker.processedFeatures}")
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
//    val tracker = JobTracker.instance(modpack)

}