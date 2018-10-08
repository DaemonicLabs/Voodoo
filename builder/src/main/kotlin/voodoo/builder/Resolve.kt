package voodoo.builder

import com.skcraft.launcher.model.ExtendedFeaturePattern
import com.skcraft.launcher.model.modpack.Feature
import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging
import voodoo.data.curse.DependencyType
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.memoize
import voodoo.provider.Providers
import voodoo.util.pool
import java.io.File
import java.util.Collections
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

private val logger = KotlinLogging.logger {}

private fun ModPack.getDependenciesCall(entryId: String): List<Entry> {
    val modpack = this
    val entry = modpack.findEntryById(entryId) ?: return emptyList()
    val result = mutableListOf(entry)
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
    val entryFeature = entry.feature ?: return
    val featureName = entry.name.takeIf { it.isNotBlank() } ?: defaultName
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
        processFeature(this, feature)
        features += feature
        entry.optional = true
    }
    logger.debug("processed ${entry.id}")
}

/**
 * iterates through all entries and set
 */
private fun processFeature(modPack: ModPack, feature: ExtendedFeaturePattern) {
    logger.info("processing feature: $feature")
    var processedEntries = emptyList<String>()
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
            depNames = depNames.filter { d ->
                modPack.entrySet.any { entry -> entry.id == d && entry.optional }
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

suspend fun resolve(
    modPack: ModPack,
    folder: File,
    updateAll: Boolean = false,
    updateDependencies: Boolean = false,
    updateEntries: List<String> = listOf()
) {
//    this.loadEntries(folder)
    modPack.loadLockEntries(folder)

    val srcDir = folder.resolve(modPack.sourceDir)

    if (updateAll) {
        modPack.lockEntrySet.clear()
        // delete all lockfiles
        folder.walkTopDown().asSequence()
            .filter {
                it.isFile && it.name.endsWith(".lock.hjson")
            }
            .forEach {
                it.delete()
            }
    } else {
        for (entryId in updateEntries) {
            val entry = modPack.findEntryById(entryId)
            if (entry == null) {
                logger.error("entry $entryId not found")
                exitProcess(-1)
            }
            modPack.lockEntrySet.find { it.id == entryId }?.let {
                it.serialFile.delete()
                modPack.lockEntrySet.remove(it)
            }
        }
    }

    if (updateDependencies || updateAll) {
        // remove all transient entries
        modPack.lockEntrySet.removeIf { (id, _) ->
            modPack.findEntryById(id)?.transient ?: true
        }
    }

    // recalculate all dependencies
    var unresolved: Set<Entry> = modPack.entrySet.toSet()
    val resolved: MutableSet<String> = Collections.synchronizedSet(mutableSetOf<String>())
//    val accumulatorContext = newSingleThreadContext("AccumulatorContext")

    do {
        val newEntriesChannel = Channel<Pair<Entry, String>>(Channel.UNLIMITED)

        logger.info("unresolved: ${unresolved.map { it.id }}")

        coroutineScope {
            for (entry in unresolved) {
                launch(context = pool + CoroutineName("job-${entry.id}")) {
                    logger.info("resolving: ${entry.id}")
                    val provider = Providers[entry.provider]

                    val lockEntry = provider.resolve(entry, modPack.mcVersion, newEntriesChannel)
                    logger.debug("received locked entry: $lockEntry")

                    logger.debug("validating: $lockEntry")
                    if (!provider.validate(lockEntry)) {
                        throw IllegalStateException("did not pass validation")
                    }

                    logger.debug("trying to merge entry")
                    val actualLockEntry = modPack.addOrMerge(lockEntry) { old, new ->
                        old ?: new
                    }
                    logger.debug("merged entry: $actualLockEntry")

                    logger.debug("validating: actual $actualLockEntry")
                    if (!provider.validate(actualLockEntry)) {
                        logger.error { actualLockEntry }
                        throw IllegalStateException("actual entry did not validate")
                    }

//                    logger.debug("setting display name")
//                    actualLockEntry.name = actualLockEntry.name()

                    logger.debug("adding to resolved")
                    resolved += entry.id

                    logger.debug("resolved: $resolved\n")
                    logger.debug("unresolved: ${modPack.entrySet.asSequence().map { entry -> entry.id }.filter { id ->
                        !resolved.contains(
                            id
                        )
                    }.toList()}\n")
                }.also {
                    logger.info("started job resolve ${entry.id}")
                }
            }
        }

        newEntriesChannel.close()
        val newEntries = mutableSetOf<Entry>()
        loop@ for ((entry, path) in newEntriesChannel) {
            logger.info("channel received: ${entry.id}")

            when {
                entry.id in resolved -> {
                    logger.info("entry already resolved ${entry.id}")
                    continue@loop
                }
                modPack.entrySet.any { it.id == entry.id } -> {
                    logger.info("entry already added ${entry.id}")
                    continue@loop
                }
                newEntries.any { it.id == entry.id } -> {
                    logger.info("entry already in queue ${entry.id}")
                    continue@loop
                }
            }

            modPack.addEntry(entry, dependency = true)
            logger.info { "added entry ${entry.id}" }
            newEntries += entry
        }
        logger.info("added last step: ${newEntries.map { it.id }}")

        logger.info("resolved last step: ${unresolved.map { it.id }}")

        unresolved = modPack.entrySet.asSequence().filter { !resolved.contains(it.id) }.toSet()
    } while (unresolved.isNotEmpty())

    val unresolvedIDs = resolved - modPack.entrySet.map { it.id }
    logger.info("unresolved ids: $unresolvedIDs")
    logger.info("resolved ids: ${modPack.lockEntrySet.map { it.id }}")

    modPack.features.clear()

    modPack.entrySet.filter {
        modPack.findLockEntryById(it.id) == null
    }.takeUnless { it.isEmpty() }?.let {
        throw IllegalStateException("unresolved entries: $it")
    }

    for (entry in modPack.entrySet) {
        modPack.resolveFeatureDependencies(
            entry, modPack.findLockEntryById(entry.id)?.name
                ?: throw NullPointerException("cannot find lockentry for ${entry.id}")
        )
    }

    // resolve features
    for (feature in modPack.features) {
        logger.info("processed feature ${feature.feature.name}")
        for (id in feature.entries) {
            logger.info("processing feature entry $id")
            val dependencies = modPack.getDependencies(id)
            feature.entries += dependencies.asSequence().filter {
                logger.debug("testing ${it.id}")
                it.optional && !feature.entries.contains(it.id)
            }.map { it.id }
        }
        logger.info("build entry: ${feature.entries.first()}")
        val mainEntry = modPack.findEntryById(feature.entries.first())!!
        feature.feature.description = mainEntry.description

        logger.info("processed feature $feature")
    }

    // TODO: rethink history, since packs are now mainly file based
}
