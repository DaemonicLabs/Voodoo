package voodoo.builder

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.provider.Providers
import voodoo.util.withPool
import java.util.Collections
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

private val logger = KotlinLogging.logger {}

suspend fun resolve(
    modPack: ModPack,
    noUpdate: Boolean = false,
    updateEntries: List<String> = listOf()
) {
//    this.loadEntries(folder)
    modPack.loadLockEntries()

    val srcDir = modPack.rootDir.resolve(modPack.sourceDir)

    if (!noUpdate) {
        modPack.lockEntrySet.clear()
        // delete all lockfiles
        srcDir.walkTopDown().asSequence()
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

    if (!noUpdate) {
        // remove all transient entries
        modPack.lockEntrySet.removeIf { (id) ->
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

        withPool { pool ->
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

                        logger.debug("resolved: $resolved")
                        logger.debug("unresolved: ${modPack.entrySet.asSequence().map { entry -> entry.id }.filter { id ->
                            !resolved.contains(
                                id
                            )
                        }.toList()}")
                    }.also {
                        logger.info("started job resolve ${entry.id}")
                    }
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

    modPack.entrySet.filter {
        modPack.findLockEntryById(it.id) == null
    }.takeUnless { it.isEmpty() }?.let {
        throw IllegalStateException("unresolved entries: $it")
    }
//    exitProcess(-1)

    // TODO: rethink history, since packs are now mainly file based
}
