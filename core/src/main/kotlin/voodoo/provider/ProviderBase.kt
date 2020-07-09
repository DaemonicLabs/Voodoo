package voodoo.provider

import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.channels.SendChannel
import mu.KLogging
import voodoo.data.DependencyType
import voodoo.data.EntryReportData
import voodoo.data.flat.Entry
import voodoo.data.lock.LockEntry
import java.io.File
import java.time.Instant

/**
 * Created by nikky on 04/01/18.
 * @author Nikky
 */

abstract class ProviderBase(
    open val name: String
) {
    val id: String
        get() = Providers.getId(this)!!

    override fun toString() = "name: $name, id: $id"

    open fun reset() {}

    open suspend fun resolve(entry: Entry, mcVersion: String, addEntry: SendChannel<Pair<Entry, String>>): LockEntry {
        logger.info("[$name] resolve ${entry.id}")
        throw NotImplementedError("unable to resolve")
    }

    companion object : KLogging()

    /**
     * downloads a entry
     *
     * @param entry the entry oyu are working on
     * @param targetFolder provided target rootFolder/location
     * @param cacheDir prepared cache directory
     */
    abstract suspend fun download(
        stopwatch: Stopwatch,
        entry: LockEntry,
        targetFolder: File,
        cacheDir: File
    ): Pair<String?, File>?

    abstract suspend fun generateName(entry: LockEntry): String

    open suspend fun getAuthors(entry: LockEntry): List<String> {
        return emptyList()
    }

    open suspend fun getProjectPage(entry: LockEntry): String {
        return ""
    }

    open suspend fun getVersion(entry: LockEntry): String {
        return ""
    }

    open suspend fun getLicense(entry: LockEntry): String {
        return ""
    }

    open suspend fun getThumbnail(entry: LockEntry): String {
        return ""
    }

    open suspend fun getThumbnail(entry: Entry): String {
        return ""
    }

    open suspend fun getReleaseDate(entry: LockEntry): Instant? {
        return null
    }

    open fun reportData(entry: LockEntry): MutableMap<EntryReportData, String> {
        return mutableMapOf(
            EntryReportData.ID to entry.id
        ).also { data ->
            data[EntryReportData.VERSION] = entry.version()
            data[EntryReportData.PROVIDER] = entry.provider
            entry.fileName?.let { fileName ->
                data[EntryReportData.FILE_NAME] = fileName
            }
            data[EntryReportData.SIDE] = "${entry.side}"
            entry.description?.let {
                data[EntryReportData.DESCRIPTION] = it
            }
            data[EntryReportData.OPTIONAL] = "${entry.optional}"
            entry.dependencies.takeIf { it.isNotEmpty() }?.let { dependencies ->

                dependencies.filterValues { it == DependencyType.REQUIRED }
                    .keys.toList().takeIf { it.isNotEmpty() }
                    ?.let { required ->
                        data[EntryReportData.DEPENDENCIES_REQUIRED] = required.sorted().joinToString(", ")
                    }
                dependencies.filterValues { it == DependencyType.OPTIONAL }
                    .keys.toList().takeIf { it.isNotEmpty() }
                    ?.let { optional ->
                        data[EntryReportData.DEPENDENCIES_OPTIONAL] = optional.sorted().joinToString(", ")
                    }
//                dependencies[DependencyType.EmbeddedLibrary]?.takeIf { it.isNotEmpty() }?.let { required ->
//                    list += Triple("dependencies_embedded", "Embedded Dependencies", required.joinToString(", ", "", ""))
//                }
//                dependencies[DependencyType.Include]?.takeIf { it.isNotEmpty() }?.let { required ->
//                    list += Triple("dependencies_include", "Include Dependencies", required.joinToString(", ", "", ""))
//                }
//                dependencies[DependencyType.Tool]?.takeIf { it.isNotEmpty() }?.let { required ->
//                    list += Triple("dependencies_tool", "Tool Dependencies", required.joinToString(", ", "", ""))
//                }
            }
        }
    }

    open fun validate(lockEntry: LockEntry): Boolean {
        if (lockEntry.id.isEmpty()) {
            logger.error("invalid id of $lockEntry")
            return false
        }
        return true
    }
}