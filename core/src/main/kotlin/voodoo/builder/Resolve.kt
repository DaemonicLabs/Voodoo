package voodoo.builder

import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import org.slf4j.MDC
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.provider.Providers
import voodoo.util.withPool
import java.util.Collections

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

private val logger = KotlinLogging.logger {}

suspend fun resolve(
    stopwatch: Stopwatch,
    modPack: ModPack
) = stopwatch {

    // remove all transient entries
    modPack.lockEntrySet.removeIf { entry ->
        modPack.findEntryById(entry.id)?.transient ?: true
    }

    // recalculate all dependencies
    var unresolved: Set<Entry> = modPack.entrySet.toSet()
    val resolved: MutableSet<String> = Collections.synchronizedSet(mutableSetOf<String>())
//    val accumulatorContext = newSingleThreadContext("AccumulatorContext")

    "resolveLoop".watch {
        do {
            val newEntriesChannel = Channel<Pair<Entry, String>>(Channel.UNLIMITED)

            logger.info("unresolved: ${unresolved.map { it.id }}")

            withPool { pool ->
                "loop unresolved".watch {
                    coroutineScope {
                        for (entry in unresolved) {
                            withLoggingContext("entry-id" to entry.id) {
                                launch(MDCContext() + CoroutineName("job-${entry.id}") + pool) {
                                    "job-${entry.id}".watch {
                                        logger.info("resolving: ${entry.id}")
                                        logger.trace { "processing: $entry" }
                                        val provider = voodoo.provider.Providers.forEntry(entry)!!

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

//                                    logger.debug("adding to resolved")
                                        resolved += entry.id

//                                    logger.debug("resolved: $resolved")
                                        val unresolvedEntries =
                                            modPack.entrySet.asSequence().map { entry -> entry.id }.filter { id ->
                                                !resolved.contains(
                                                    id
                                                )
                                            }.toList()
                                        logger.debug("unresolved: ${unresolvedEntries}")
                                    }

                                }.also {
                                    logger.info("started job resolve ${entry.id}")
                                }
                            }

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
    }

    val unresolvedIDs = resolved - modPack.entrySet.map { it.id }
    logger.info("unresolved ids: $unresolvedIDs")
    logger.info("resolved ids: ${modPack.lockEntrySet.map { it.id }}")

    modPack.entrySet.filter {
        modPack.findLockEntryById(it.id) == null
    }.takeUnless { it.isEmpty() }?.let {
        throw IllegalStateException("unresolved entries: $it")
    }
}
