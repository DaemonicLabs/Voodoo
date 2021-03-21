package voodoo.changelog

import Modloader
import mu.KotlinLogging
import voodoo.data.DependencyType
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.markdownTable
import voodoo.util.blankOr
import voodoo.util.toRelativeUnixPath

open class ChangelogBuilder {
    private val logger = KotlinLogging.logger {}
    open val filename: String = "changelog.md"
    open val fullFilename: String = "complete_changelog.md"

    /***
     * main function generating the complete changelog
     */
    open fun StringBuilder.writeFullChangelog(
        steps: List<Pair<LockPack, LockPack>>,
    ) {
        // TODO: write first
        writeChangelog(oldPack = null, newPack = steps.first().second)
        steps.dropLast(1).forEach { (oldPack, newPack) ->
            appendLine()
            appendLine()
            appendLine()
            writeChangelog(oldPack = oldPack, newPack = newPack)
        }
    }

    /***
     * main function generating the changelog
     */
    open fun StringBuilder.writeChangelog(
        oldPack: LockPack?,
        newPack: LockPack,
    ) {
        writePackInfo(newPack = newPack, oldPack = oldPack)

        // TODO: use lockpacks too
        writeEntries(newPack = newPack, oldPack = oldPack)
    }

    //    open fun StringBuilder.writePackInfo(
//        newMetaInfo: Map<PackReportData, String>,
//        oldMetaInfo: Map<PackReportData, String>
//    ) {
    open fun StringBuilder.writePackInfo(
        newPack: LockPack,
        oldPack: LockPack?,
    ) {
        val title = "# ${newPack.title.blankOr ?: newPack.id} ${newPack.version}"
        appendLine(title)

        val table = if (oldPack == null) {
            val modloaderTableEntries = when (val loader = newPack.modloader) {
                is Modloader.Forge -> {
                    // TODO: add mcversion and branch ?
                    arrayOf("Forge Version" to loader.forgeVersion)
                }
                is Modloader.Fabric -> {
                    arrayOf(
                        "Fabric Intermediary Version" to loader.intermediateMappings,
                        "Fabric Loader Version" to loader.loader,
                        "Fabric Installer Version" to loader.installer,
                    )
                }
                else -> arrayOf()
            }

            val iconTableEntries = if (newPack.iconFile.exists()) {
                arrayOf(
                    "Icon src" to newPack.iconFile.relativeTo(newPack.lockBaseFolder).path,
                    "Icon" to "<img src=\"${newPack.iconFile.relativeTo(newPack.lockBaseFolder).path}\" alt=\"icon\" style=\"max-height: 128px;\"/>"
                )
            } else arrayOf()

            val tableContent = listOf<Pair<String, String?>>(
                "Title" to newPack.title,
                "Pack Version" to newPack.version,
                "MC Version" to newPack.mcVersion,
                "Authors" to newPack.authors.joinToString(", "),
                *modloaderTableEntries,
                *iconTableEntries
            )

            markdownTable(
                headers = listOf("Property", "Value"),
                content = tableContent.map { (first, second) ->
                    listOf(first, second.toString())
                }
            )
        } else {
            val tableContent = listOf<Triple<String, String?, String?>>(
                "Title"
                        to oldPack.title
                        to newPack.title,
                "Pack Version"
                        to oldPack.title
                        to newPack.version,
                "MC Version"
                        to oldPack.mcVersion
                        to newPack.mcVersion,
                "Authors"
                        to oldPack.authors.joinToString(", ")
                        to newPack.authors.joinToString(", "),
                "Forge Version"
                        to (oldPack.modloader as? Modloader.Forge)?.forgeVersion
                        to (newPack.modloader as? Modloader.Forge)?.forgeVersion,
                "Fabric Intermediary Version"
                        to (oldPack.modloader as? Modloader.Fabric)?.intermediateMappings
                        to (newPack.modloader as? Modloader.Fabric)?.intermediateMappings,
                "Fabric Loader Version"
                        to (oldPack.modloader as? Modloader.Fabric)?.loader
                        to (newPack.modloader as? Modloader.Fabric)?.loader,
                "Fabric Installer Version"
                        to (oldPack.modloader as? Modloader.Fabric)?.installer
                        to (newPack.modloader as? Modloader.Fabric)?.installer,
                "Icon src"
                        to oldPack.takeIf { it.iconFile.exists() }?.let {
                    it.iconFile.toRelativeUnixPath(it.lockBaseFolder)
                }
                        to newPack.takeIf { it.iconFile.exists() }?.let {
                    it.iconFile.toRelativeUnixPath(it.lockBaseFolder)
                },
                "Icon"
                        to newPack.takeIf { it.iconFile.exists() }?.let {
                    "<img src=\"${it.iconFile.toRelativeUnixPath(it.lockBaseFolder)}\" alt=\"icon\" style=\"max-height: 128px;\"/>"
                }
                        to oldPack.takeIf { it.iconFile.exists() }?.let {
                    "<img src=\"${it.iconFile.toRelativeUnixPath(it.lockBaseFolder)}\" alt=\"icon\" style=\"max-height: 128px;\"/>"
                }
            )

            diffTable(
                newMetaInfo = tableContent.associate { (key, _, new) -> key to new.toString() },
                oldMetaInfo = tableContent.associate { (key, old, _) -> key to old.toString() },
            )
        }
        table?.let {
            appendLine()
            append(it)
        }
    }

    private infix fun <A, B, C> Pair<A, B>.to(c: C) = Triple(first, second, c)

    open fun StringBuilder.writeEntries(
        newPack: LockPack,
        oldPack: LockPack?,
    ) {
        val newEntries = newPack.entriesMap
        val oldEntries = oldPack?.entriesMap ?: mapOf()

        val addedEntries = newEntries
            .filterKeys { id ->
                !oldEntries.containsKey(id)
            }
            .map { (id, newEntry) -> addedEntry(newEntry) }
            .filterNotNull()
        val removedEntries = oldEntries
            .filterKeys { id ->
                !newEntries.containsKey(id)
            }
            .map { (id, removedOldEntry) ->
                removedEntry(removedOldEntry)
            }
            .filterNotNull()
        val changedEntries = newEntries
            .mapNotNull { (id, newEntry) ->
                oldEntries[id]?.let { oldEntry ->
                    updatedEntry(newEntry = newEntry, oldEntry = oldEntry)
                }
            }

        if (addedEntries.isNotEmpty() || removedEntries.isNotEmpty() || changedEntries.isNotEmpty()) {
            appendLine()
            appendLine()
            appendLine("## Entries")
            appendLine()

            if (addedEntries.isNotEmpty()) {
                appendLine("### Added Entries")
                addedEntries.forEach { content ->
                    appendLine()
                    appendLine(content)
                }
                appendLine()
            }

            if (changedEntries.isNotEmpty()) {
                appendLine("### Updated Entries")
                changedEntries.forEach { content ->
                    appendLine()
                    appendLine(content)
                }
                appendLine()
            }

            if (removedEntries.isNotEmpty()) {
                appendLine("### Removed Entries")
                removedEntries.forEach { content ->
                    appendLine()
                    appendLine(content)
                }
                appendLine()
            }
        } else {
            appendLine()
            appendLine("No change in entries")
            appendLine()
        }
    }

    //    open fun addedEntry(id: String, metaInfo: Map<EntryReportData, String>): String? {
    open fun addedEntry(entry: LockEntry): String? {
        logger.debug { "added entry: ${entry.id}" }
        return diffTableEntry(newEntry = entry, oldEntry = null)
            ?.let { t ->
                buildString {
                    appendLine("added `${entry.id}`")
                    appendLine()
                    appendLine(t)
                }
            }
    }

    open fun removedEntry(entry: LockEntry): String? {
        logger.debug { "removed entry: ${entry.id}" }
        return diffTableEntry(newEntry = null, oldEntry = entry)
            ?.let { t ->
                buildString {
                    appendLine("removed `${entry.id}`")
                    appendLine()
                    appendLine(t)
                }
            }
    }

    open fun updatedEntry(
        newEntry: LockEntry,
        oldEntry: LockEntry,
    ): String? {
        logger.debug { "updated entry: ${newEntry.id}, $oldEntry -> $newEntry" }
        return diffTableEntry(newEntry = newEntry, oldEntry = oldEntry)
            ?.let { t ->
                buildString {
                    appendLine("updated `${newEntry.id}`")
                    appendLine()
                    appendLine(t)
                }
            }
    }

    protected fun diffTableEntry(
        propHeader: String = "Property",
        oldheader: String = "old value",
        newheader: String = "new value",
        newEntry: LockEntry?,
        oldEntry: LockEntry?,
    ): String? {
        val tableContent: Map<String, Pair<Any?, Any?>> = listOf(
            "ID"
                    to oldEntry?.id
                    to newEntry?.id,
            "Version"
                    to oldEntry?.version()
                    to newEntry?.version(),
            "Type"
                    to oldEntry?.providerType
                    to newEntry?.providerType,
            "File Name"
                    to oldEntry?.fileName
                    to newEntry?.fileName,
            "Side"
                    to oldEntry?.side
                    to newEntry?.side,
            "Description"
                    to oldEntry?.description
                    to newEntry?.description,
            "Optional"
                    to oldEntry?.optional
                    to newEntry?.optional,
            "Required Dependencies"
                    to oldEntry?.dependencies?.filterValues { it == DependencyType.REQUIRED }
                ?.keys?.takeIf { it.isNotEmpty() }
                ?.sorted()?.joinToString(", ")
                    to newEntry?.dependencies?.filterValues { it == DependencyType.REQUIRED }
                ?.keys?.takeIf { it.isNotEmpty() }
                ?.sorted()?.joinToString(", "),
            "Optional Dependencies"
                    to oldEntry?.dependencies?.filterValues { it == DependencyType.OPTIONAL }
                ?.keys?.takeIf { it.isNotEmpty() }
                ?.sorted()?.joinToString(", ")
                    to newEntry?.dependencies?.filterValues { it == DependencyType.OPTIONAL }
                ?.keys?.takeIf { it.isNotEmpty() }
                ?.sorted()?.joinToString(", ")
        ).associate { (key, old, new) -> key to Pair(old, new) }

        val oldEntryTableOverrides =
            oldEntry?.let { entry -> entry.provider().generateReportTableOverrides(entry) } ?: mapOf()
        val newEntryTableOverrides =
            newEntry?.let { entry -> entry.provider().generateReportTableOverrides(entry) } ?: mapOf()

        val mergedTableOverrides = (oldEntryTableOverrides.keys + newEntryTableOverrides.keys).associate { key ->
            key to Pair(oldEntryTableOverrides[key], newEntryTableOverrides[key])
        }

        val fullTableContent = tableContent
            .mapValues { (key, pair) ->
                val (old, new) = pair
                val oldValue = oldEntryTableOverrides.getOrDefault(key, null) ?: old
                val newValue = newEntryTableOverrides.getOrDefault(key, null) ?: new

                Pair(oldValue, newValue)
            } +
                mergedTableOverrides
                    .filter { (key, _) -> key !in tableContent }
                    .mapValues { (_, pair) ->
                        val (oldValue, newValue) = pair
                        Pair(oldValue, newValue)
                    }


        return diffTable(
            propHeader = propHeader,
            oldheader = oldheader,
            newheader = newheader,
            newMetaInfo = fullTableContent.mapValues { (_, pair) ->
                val (_, new) = pair
                new.toString()
            },
            oldMetaInfo = fullTableContent.mapValues { (_, pair) ->
                val (old, _) = pair
                old.toString()
            },
        )
    }

    /***
     * generates a markdown table
     */
    open fun diffTable(
        propHeader: String = "Property",
        oldheader: String = "old value",
        newheader: String = "new value",
        newMetaInfo: Map<String, String>,
        oldMetaInfo: Map<String, String>,
    ): String? {
        val content = mutableListOf<List<String>>()
        newMetaInfo.forEach { key, newInfo ->
            val oldInfo = oldMetaInfo[key]
            if (oldInfo != null) {
                // value was changed
                if (oldInfo != newInfo) {
                    content += listOf(key, oldInfo, newInfo)
                }
            } else {
                // value was added
                content += listOf(key, "", newInfo)
            }
        }

        oldMetaInfo.filter { (key, meta) -> !newMetaInfo.containsKey(key) }
            .forEach { key, oldInfo ->
                // value was removed
                content += listOf(key, oldInfo, "")
            }

        if (content.isEmpty()) {
            logger.debug("empty table because there was no differences btween")
            logger.debug { "old: $oldMetaInfo" }
            logger.debug { "new: $newMetaInfo" }
            return null
        }

        return markdownTable(
            headers = listOf(propHeader, oldheader, newheader),
            content = content.toList()
        ) + "\n"
    }
}