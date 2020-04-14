package voodoo.changelog

import mu.KLogging
import voodoo.data.EntryReportData
import voodoo.data.PackReportData
import voodoo.markdownTable
import voodoo.util.blankOr

open class ChangelogBuilder : KLogging() {
    open val filename: String = "changelog.md"
    open val fullFilename: String = "complete_changelog.md"

    /***
     * main function generating the complete changelog
     */
    open fun StringBuilder.writeFullChangelog(
        steps: List<ChangelogStepData>
    ) {
        steps.dropLast(1).forEach { step->
            writeChangelog(step)
            appendln()
            appendln()
            appendln()
        }
        writeChangelog(steps.last())
    }

    /***
     * main function generating the changelog
     */
    open fun StringBuilder.writeChangelog(
        step: ChangelogStepData
    ) {
        writePackInfo(step.newPackMetaInfo, step.oldPackMetaInfo)

        writeEntries(step.newEntryMetaInfo, step.oldEntryMetaInfo)
    }

    open fun StringBuilder.writePackInfo(
        newMetaInfo: Map<PackReportData, String>,
        oldMetaInfo: Map<PackReportData, String>
    ) {
        val title = "# ${newMetaInfo[PackReportData.TITLE].blankOr ?: newMetaInfo[PackReportData.ID]} ${newMetaInfo[PackReportData.VERSION]}"
        appendln(title)
        val table = if (oldMetaInfo.isEmpty()) {
            markdownTable(headers = listOf("Property", "Value"), content = newMetaInfo.map { (key, meta) ->
                listOf(key.humanReadable, meta)
            })
        } else {
            diffTablePack(newMetaInfo = newMetaInfo, oldMetaInfo = oldMetaInfo)
        }
        table?.let {
            appendln()
            append(it)
        }
    }

    open fun StringBuilder.writeEntries(
        newMetaInfo: Map<String, Map<EntryReportData, String>>,
        oldMetaInfo: Map<String, Map<EntryReportData, String>>
    ) {
        val addedEntries = newMetaInfo
            .filter { (id, _) ->
                !oldMetaInfo.containsKey(id)
            }
            .map { (id, metaInfo) -> addedEntry(id, metaInfo) }
            .filterNotNull()
        val removedEntries = oldMetaInfo
            .filter { (id, _) ->
                !newMetaInfo.containsKey(id)
            }
            .map { (id, metaInfo) ->
                removedEntry(id = id, metaInfo = metaInfo)
            }
            .filterNotNull()
        val changedEntries = newMetaInfo
            .filter { (id, _) ->
                oldMetaInfo.containsKey(id)
            }.mapValues { (id, metaInfo) ->
                oldMetaInfo.getValue(id) to metaInfo
            }
            .map { (id, pair) ->
                val (old, new) = pair
                updatedEntry(id = id, newMetaInfo = new, oldMetaInfo = old)
            }
            .filterNotNull()

        if (addedEntries.isNotEmpty() || removedEntries.isNotEmpty() || changedEntries.isNotEmpty()) {
            appendln()
            appendln()
            appendln("## Entries")
            appendln()

            if (addedEntries.isNotEmpty()) {
                appendln("### Added Entries")
                addedEntries.forEach { content ->
                    appendln()
                    appendln(content)
                }
                appendln()
            }

            if (changedEntries.isNotEmpty()) {
                appendln("### Updated Entries")
                changedEntries.forEach { content ->
                    appendln()
                    appendln(content)
                }
                appendln()
            }

            if (removedEntries.isNotEmpty()) {
                appendln("### Removed Entries")
                removedEntries.forEach { content ->
                    appendln()
                    appendln(content)
                }
                appendln()
            }
        } else {
            appendln()
            appendln("No change in entries")
            appendln()
        }
    }

    open fun addedEntry(id: String, metaInfo: Map<EntryReportData, String>): String? {
        logger.debug { "added entry: $id, $metaInfo" }
        return diffTableEntry(newMetaInfo = metaInfo, oldMetaInfo = mapOf())
            ?.let { t ->
                buildString {
                    appendln("added `$id`")
                    appendln()
                    appendln(t)
                }
            }
    }

    open fun removedEntry(id: String, metaInfo: Map<EntryReportData, String>): String? {
        logger.debug { "removed entry: $id, $metaInfo" }
        return diffTableEntry(newMetaInfo = mapOf(), oldMetaInfo = metaInfo)
            ?.let { t ->
                buildString {
                    appendln("removed `$id`")
                    appendln()
                    appendln(t)
                }
            }
    }

    open fun updatedEntry(id: String, newMetaInfo: Map<EntryReportData, String>, oldMetaInfo: Map<EntryReportData, String>): String? {
        logger.debug { "updated entry: $id, $oldMetaInfo -> $newMetaInfo" }
        return diffTableEntry(newMetaInfo = newMetaInfo, oldMetaInfo = oldMetaInfo)
            ?.let { t ->
                buildString {
                    appendln("updated `$id`")
                    appendln()
                    appendln(t)
                }
            }
    }

    protected fun diffTableEntry(
        propHeader: String = "Property",
        oldheader: String = "old value",
        newheader: String = "new value",
        newMetaInfo: Map<EntryReportData, String>,
        oldMetaInfo: Map<EntryReportData, String>
    ) = diffTable(
        propHeader = propHeader,
        oldheader = oldheader,
        newheader = newheader,
        newMetaInfo = newMetaInfo.mapKeys { (reportData, _) -> reportData.humanReadable },
        oldMetaInfo = oldMetaInfo.mapKeys { (reportData, _) -> reportData.humanReadable }
    )

    protected fun diffTablePack(
        propHeader: String = "Property",
        oldheader: String = "old value",
        newheader: String = "new value",
        newMetaInfo: Map<PackReportData, String>,
        oldMetaInfo: Map<PackReportData, String>
    ) = diffTable(
        propHeader = propHeader,
        oldheader = oldheader,
        newheader = newheader,
        newMetaInfo = newMetaInfo.mapKeys { (reportData, _) -> reportData.humanReadable },
        oldMetaInfo = oldMetaInfo.mapKeys { (reportData, _) -> reportData.humanReadable }
    )

    /***
     * generates a markdown table
     */
    open fun diffTable(
        propHeader: String = "Property",
        oldheader: String = "old value",
        newheader: String = "new value",
        newMetaInfo: Map<String, String>,
        oldMetaInfo: Map<String, String>
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