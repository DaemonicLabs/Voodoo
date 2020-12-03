package voodoo.builder

import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.data.flat.FlatEntry
import voodoo.data.flat.FlatModPack
import voodoo.data.lock.LockEntry
import voodoo.util.withPool
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

private val logger = KotlinLogging.logger {}

suspend fun resolve(
    stopwatch: Stopwatch,
    modPack: FlatModPack,
): Set<LockEntry> = stopwatch {

    val mutableEntryMap = ConcurrentHashMap<String, FlatEntry>()

    fun addOrMerge(entry: FlatEntry, mergeOp: (FlatEntry, FlatEntry) -> FlatEntry): FlatEntry {
        val result = mutableEntryMap[entry.id]?.let { existing ->
            mergeOp(existing, entry)
        } ?: entry

        mutableEntryMap[entry.id] = result

        return result
    }

    val mutableLockEntryMap = ConcurrentHashMap<String, LockEntry>()

    fun addOrMerge(
        entry: LockEntry,
        mergeOp: (old: LockEntry?, new: LockEntry) -> LockEntry = { old, new -> old ?: new },
    ): LockEntry {
        val result = mutableLockEntryMap[entry.id]?.let { existing ->
            mergeOp(existing, entry)
        } ?: entry

        mutableLockEntryMap[entry.id] = result

        return result
    }


    fun addEntry(entry: FlatEntry, dependency: Boolean = false) {
        if (entry.id.isBlank()) {
            logger.error("invalid: $entry")
            return
        }

        addOrMerge(entry) { existingEntry, newEntry ->
            if (!(newEntry === existingEntry)) {
                logger.info("duplicate entry $newEntry")
                logger.info("old entry $existingEntry")

                if (!dependency && !existingEntry.transient) {
                    throw IllegalStateException("duplicate entries: ${newEntry.folder} ${newEntry.serialFilename} and ${existingEntry.folder} ${existingEntry.serialFilename}")
                }

                // TODO: make some util code to merge content of Entries
                // is this picked up if the entry was already processed and resolved ?
                existingEntry.side += newEntry.side
                if (existingEntry.optionalData == null) {
                    if (newEntry.optionalData != null) {
                        logger.warn { "copying optionalData of ${newEntry.id} to ${existingEntry.id} ${newEntry.optionalData}" }
                    }
                    existingEntry.optionalData = newEntry.optionalData
                }
                if (existingEntry.description?.isBlank() == true) {
                    existingEntry.description = newEntry.description
                }

                existingEntry
            } else {
                newEntry
            }
        }
    }

    "resolveLoop".watch {
        do {
            val newEntriesChannel = Channel<Pair<FlatEntry, String>>(Channel.UNLIMITED)

            val unresolvedEntries = modPack.entrySet.filterNot { entry -> mutableLockEntryMap.containsKey(entry.id) }

            logger.info("unresolved: ${unresolvedEntries.map { it.id }}")

            val resolved: List<LockEntry> = withPool { pool ->
                "loop unresolved".watch {
                    coroutineScope {
                        unresolvedEntries.map { entry ->
                            withLoggingContext("entry-id" to entry.id) {
                                async(MDCContext() + CoroutineName("job-${entry.id}") + pool) {
                                    "job-${entry.id}".watch {
                                        logger.info { "resolving: ${entry.id}" }
                                        logger.trace { "processing: $entry" }
                                        val provider = voodoo.provider.Providers.forEntry(entry)!!

                                        entry.takeUnless { it is FlatEntry.Noop }?.let { entry ->
                                            val lockEntry = provider.resolve(entry, modPack.mcVersion, newEntriesChannel)
                                            logger.debug("received locked entry: $lockEntry")

                                            logger.debug("validating: $lockEntry")
                                            if (!provider.validate(lockEntry)) {
                                                throw IllegalStateException("did not pass validation")
                                            }

//                                            logger.debug("setting display name")
//                                            actualLockEntry.name = actualLockEntry.name()

                                            lockEntry
                                        }
                                    }
                                }.also {
                                    logger.info("started job resolve ${entry.id}")
                                }
                            }
                        }.awaitAll()
                            .filterNotNull()
                    }
                }
            }

            resolved.forEach { lockEntry ->
                // inserts already ?
                val actualLockEntry: LockEntry = addOrMerge(lockEntry) { old, new ->
                    old ?: new
                }
                logger.debug("merged entry: $actualLockEntry")

//                logger.debug("validating: actual $actualLockEntry")
//                val provider = voodoo.provider.Providers.forEntry(entry)!!
//                if (!provider.validate(actualLockEntry)) {
//                    logger.error { actualLockEntry }
//                    throw IllegalStateException("actual entry did not validate")
//                }
            }

            newEntriesChannel.close()
            val newEntries = mutableSetOf<FlatEntry>()
            for ((entry, path) in newEntriesChannel) {
                logger.debug { "new entry received: ${entry.id}" }

                when {
                    mutableLockEntryMap.containsKey(entry.id) -> {
                        logger.info { "entry already resolved ${entry.id}" }
                        continue
                    }
                    mutableEntryMap.any { (id, entry) -> id == entry.id } -> {
                        logger.info { "entry already added ${entry.id}" }
                        continue
                    }
//                    newEntries.any { it.id == entry.id } -> {
//                        logger.info("entry already in queue ${entry.id}")
//                        continue
//                    }
                }

                addEntry(entry, dependency = true)

                logger.info { "added entry ${entry.id}" }
                newEntries += entry
            }
            logger.info { "added last step: ${newEntries.map { it.id }}" }

            logger.info { "resolved last step: ${unresolvedEntries.map { it.id }}" }

        } while (unresolvedEntries.isNotEmpty())
    }

    mutableLockEntryMap.values.toSet()
}
