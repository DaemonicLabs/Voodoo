package voodoo.changelog

import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.data.meta.MetaInfo
import voodoo.markdownTable
import voodoo.util.blankOr

open class ChangelogBuilder : KLogging() {
    /***
     * main function generating the changelog
     */
    open fun StringBuilder.writeChangelog(
        newPack: LockPack,
        oldPack: LockPack?,
        newPackMetaInfo: Map<String, MetaInfo>,
        oldPackMetaInfo: Map<String, MetaInfo>,
        newEntryMetaInfo: Map<String, Map<String, MetaInfo>>,
        oldEntryMetaInfo: Map<String, Map<String, MetaInfo>>
    ) {
        writePackInfo(newPack, oldPack, newPackMetaInfo, oldPackMetaInfo)

        writeEntries(newPack, oldPack, newEntryMetaInfo, oldEntryMetaInfo)

        appendln()
        appendln()
        appendln()
    }

    open fun StringBuilder.writePackInfo(
        newPack: LockPack,
        oldPack: LockPack?,
        newMetaInfo: Map<String, MetaInfo>,
        oldMetaInfo: Map<String, MetaInfo>
    ) {
        val title = "# ${newPack.title.blankOr ?: newPack.id} ${newPack.version}"
        appendln(title)
        val table = if (oldMetaInfo.isEmpty()) {
            markdownTable(headers = listOf("Property", "Value"), content = newMetaInfo.map { (_, meta) ->
                listOf(meta.name, meta.value)
            })
        } else {
            diffTable(newMetaInfo = newMetaInfo, oldMetaInfo = oldMetaInfo)
        }
        table?.let {
            appendln()
            append(it)
        }
    }

    open fun StringBuilder.writeEntries(
        newPack: LockPack,
        oldPack: LockPack?,
        newMetaInfo: Map<String, Map<String, MetaInfo>>,
        oldMetaInfo: Map<String, Map<String, MetaInfo>>
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

    open fun addedEntry(id: String, metaInfo: Map<String, MetaInfo>): String? {
        logger.debug { "added entry: $id, $metaInfo" }
        return diffTable(newMetaInfo = metaInfo, oldMetaInfo = mapOf())
            ?.let { t ->
                buildString {
                    appendln("added `$id`")
                    appendln()
                    appendln(t)
                }
            }
    }

    open fun removedEntry(id: String, metaInfo: Map<String, MetaInfo>): String? {
        logger.debug { "removed entry: $id, $metaInfo" }
        return diffTable(newMetaInfo = mapOf(), oldMetaInfo = metaInfo)
            ?.let { t ->
                buildString {
                    appendln("removed `$id`")
                    appendln()
                    appendln(t)
                }
            }
    }

    open fun updatedEntry(id: String, newMetaInfo: Map<String, MetaInfo>, oldMetaInfo: Map<String, MetaInfo>): String? {
        logger.debug { "updated entry: $id, $oldMetaInfo -> $newMetaInfo" }
        return diffTable(newMetaInfo = newMetaInfo, oldMetaInfo = oldMetaInfo)
            ?.let { t ->
                buildString {
                    appendln("updated `$id`")
                    appendln()
                    appendln(t)
                }
            }
    }

    /***
     * generates a markdown table
     */
    protected fun diffTable(
        propHeader: String = "Property",
        oldheader: String = "old value",
        newheader: String = "new value",
        newMetaInfo: Map<String, MetaInfo>,
        oldMetaInfo: Map<String, MetaInfo>
    ): String? {
        val content = mutableListOf<List<String>>()
        newMetaInfo.forEach { key, newInfo ->
            val oldInfo = oldMetaInfo[key]
            if (oldInfo != null) {
                // value was changed
                if (oldInfo.value != newInfo.value) {
                    content += listOf(newInfo.name, oldInfo.value, newInfo.value)
                }
            } else {
                // value was added
                content += listOf(newInfo.name, "", newInfo.value)
            }
        }

        oldMetaInfo.filter { (key, meta) -> !newMetaInfo.containsKey(key) }
            .forEach { key, oldInfo ->
                // value was removed
                content += listOf(oldInfo.name, oldInfo.value, "")
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