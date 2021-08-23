package voodoo.builder

import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.data.flat.FlatEntry
import voodoo.data.flat.FlatModPack
import voodoo.data.lock.LockEntry
import voodoo.provider.ProviderBase
import voodoo.util.withPool
import java.util.*

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

private val logger = KotlinLogging.logger {}

suspend fun resolve(
    stopwatch: Stopwatch,
    modPack: FlatModPack,
): Set<LockEntry> = stopwatch {

    val mutableEntryMap = modPack.entrySet.associateBy { it.id }.toMutableMap()
    val mutableResolvedMap = mutableMapOf<String, Boolean>()

    fun addOrMerge(entry: FlatEntry, mergeOp: (existing: FlatEntry, new: FlatEntry) -> FlatEntry): FlatEntry {
        val result = mutableEntryMap[entry.id]?.let { existing ->
            mergeOp(existing, entry)
        } ?: entry

        mutableEntryMap[entry.id] = result

        return result
    }

    val mutableLockEntryMap = mutableMapOf<String, LockEntry>()

    fun addOrMergeLockEntry(
        entry: LockEntry,
        mergeOp: (existing: LockEntry?, new: LockEntry) -> LockEntry = { old, new -> old ?: new },
    ): LockEntry {
        val result = mutableLockEntryMap[entry.id]?.let { existing ->
            mergeOp(existing, entry)
        } ?: entry

        mutableLockEntryMap[entry.id] = result

        return result
    }


    fun addFlatEntry(entry: FlatEntry, isDependency: Boolean = false) {
        if (entry.id.isBlank()) {
            logger.error("invalid: $entry")
            return
        }

        addOrMerge(entry) { existingEntry, newEntry ->
            if (!(newEntry === existingEntry)) {
                logger.info("duplicate entry $newEntry")
                logger.info("old entry $existingEntry")

                if (!isDependency && !existingEntry.transient) {
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
            val unresolvedEntries = mutableEntryMap.filterKeys { id -> !mutableResolvedMap.getOrDefault(id, false) }
            coroutineScope {
                val newEntries = Collections.synchronizedList(mutableListOf<FlatEntry>())

                //TODO: add merge op, so (modrinth) provider
                suspend fun addEntry(entry: FlatEntry) {
                    logger.debug { "new entry received: ${entry.id}" }

                    if (mutableLockEntryMap.containsKey(entry.id)) {
                        logger.info { "entry already resolved ${entry.id}" }
                    } else {

                        addFlatEntry(entry, isDependency = true)

                        logger.info { "added entry ${entry.id}" }
                        newEntries += entry
                    }
                }

                logger.info("unresolved: ${unresolvedEntries.keys}")

                val resolved: Map<String, FlatEntry> = withPool { pool ->
                    "loop unresolved".watch {
                        coroutineScope {
                            unresolvedEntries.map { (id, entry) ->
                                withLoggingContext("entry-id" to entry.id) {
                                    async(MDCContext() + CoroutineName("job-${entry.id}") + pool) {
                                        "job-${entry.id}".watch {
                                            logger.info { "resolving: ${entry.id}" }
                                            logger.trace { "processing: $entry" }
                                            val provider = voodoo.provider.Providers.forEntry(entry)!!

                                            if (entry is FlatEntry.Noop) return@watch null

                                            val resolvedEntry = provider.resolve(entry, modPack, ::addEntry)
                                            logger.debug("received locked entry: $resolvedEntry")

                                            id to resolvedEntry
                                        }
                                    }
                                        .also {
                                            logger.info("started job resolve ${entry.id}")
                                        }
                                }
                            }
                                .awaitAll()
                                .filterNotNull()
                                .toMap()
                        }.also {
                            logger.trace { "unresolved-loop finished" }
                        }
                    }
                }
                resolved.forEach { (id, resolvedEntry) ->
//                    val mergedEntry: FlatEntry = addOrMerge(resolvedEntry) { old, new ->
//                        old ?: new
//                    }
//                    logger.debug("merged entry: $mergedEntry")

                    // TODO: replace or merge better ?
                    mutableEntryMap[id] = resolvedEntry
                    mutableResolvedMap[id] = true
                }

                logger.info { "added last step: ${newEntries.map { it.id }}" }

                logger.info { "resolved last step: ${unresolvedEntries.keys}" }
            }


        } while (unresolvedEntries.isNotEmpty())
    }

    "lockLoop".watch {
        // TODO: turn FlatEntries into LockEntries
        // TODO: turn into single loop ?
        do {
            val unlockedEntries = mutableEntryMap.filterKeys { id -> !mutableLockEntryMap.containsKey(id) }
            logger.info("un-locked: ${unlockedEntries.keys}")

            val lockedEntries: List<LockEntry> = withPool { pool ->
                "loop un-locked".watch {
                    coroutineScope {
                        unlockedEntries.map { (id, entry) ->
                            withLoggingContext("entry-id" to entry.id) {
                                async(MDCContext() + CoroutineName("lock-${entry.id}") + pool) {
                                    "job-${entry.id}".watch {
                                        logger.info { "locking: ${entry.id}" }
                                        logger.trace { "processing: $entry" }
                                        val provider: ProviderBase = voodoo.provider.Providers.forEntry(entry)!!

                                        if (entry is FlatEntry.Noop) return@watch null

                                        entry.takeUnless { it is FlatEntry.Noop }?.let { entry ->
                                            val lockEntry = provider.lock(entry, modPack)
                                            logger.debug("received locked entry: $lockEntry")

                                            logger.debug("validating: $lockEntry")
                                            if (!provider.validate(lockEntry)) {
                                                throw IllegalStateException("did not pass validation")
                                            }

                                            lockEntry
                                        }
                                    }
                                }
                                    .also {
                                        logger.info("started job lock ${entry.id}")
                                    }
                            }
                        }
                            .awaitAll()
                            .filterNotNull()
                    }
                }.also {
                    logger.trace { "unresolved-loop finished" }
                }
            }
            lockedEntries.forEach { lockEntry ->
                // inserts already ?
                val actualLockEntry: LockEntry = addOrMergeLockEntry(lockEntry) { old, new ->
                    old ?: new
                }
                logger.debug("merged entry: $actualLockEntry")

                logger.info { "locked last step: ${unlockedEntries.keys}" }
            }

        } while (unlockedEntries.isNotEmpty())
    }

/*
    "resolveLoop".watch {
        do {
            val unresolvedEntries = mutableEntryMap.filterKeys { id -> !mutableLockEntryMap.containsKey(id) }
            coroutineScope {
                val newEntries = Collections.synchronizedList(mutableListOf<FlatEntry>())
                suspend fun addEntry(entry: FlatEntry) {
                    logger.debug { "new entry received: ${entry.id}" }

                    if (mutableLockEntryMap.containsKey(entry.id)) {
                        logger.info { "entry already resolved ${entry.id}" }
                    } else {

                        addEntry(entry, dependency = true)

                        logger.info { "added entry ${entry.id}" }
                        newEntries += entry
                    }
                }

                logger.info("unresolved: ${unresolvedEntries.keys}")

                val resolved: List<LockEntry> = withPool { pool ->
                    "loop unresolved".watch {
                        coroutineScope {
                            unresolvedEntries.map { (id, entry) ->
                                withLoggingContext("entry-id" to entry.id) {
                                    async(MDCContext() + CoroutineName("job-${entry.id}") + pool) {
                                        "job-${entry.id}".watch {
                                            logger.info { "resolving: ${entry.id}" }
                                            logger.trace { "processing: $entry" }
                                            val provider = voodoo.provider.Providers.forEntry(entry)!!

                                            entry.takeUnless { it is FlatEntry.Noop }?.let { entry ->
                                                val lockEntry = provider.resolve(entry, modPack, ::addEntry)
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
                                    }
                                        .also {
                                            logger.info("started job resolve ${entry.id}")
                                        }
                                }
                            }.awaitAll()
                                .filterNotNull()
                        }.also {
                            logger.trace { "unresolved-loop finished" }
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
//                }

//                val newEntries = addEntries.consumeAsFlow().filter { entry ->
//                    logger.debug { "new entry received: ${entry.id}" }
//
//                    if (mutableLockEntryMap.containsKey(entry.id)) {
//                        logger.info { "entry already resolved ${entry.id}" }
//                        false
//                    } else {
//
//                        addEntry(entry, dependency = true)
//
//                        logger.info { "added entry ${entry.id}" }
//                        true
//                    }
//
//                }.toList()

                logger.info { "added last step: ${newEntries.map { it.id }}" }

                logger.info { "resolved last step: ${unresolvedEntries.keys}" }
            }


        } while (unresolvedEntries.isNotEmpty())
    }
    */

    mutableLockEntryMap.values.toSet()
}
