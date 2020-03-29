package voodoo.changelog

import kotlinx.coroutines.runBlocking
import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.forge.ForgeUtil
import voodoo.markdownTable
import voodoo.provider.Providers
import voodoo.util.json
import voodoo.util.unixPath
import java.io.File
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

val LockPack.forgeVersion: String
    get() = runBlocking {
        ForgeUtil.forgeVersionOf(forge)?.forgeVersion ?: "missing"
    }
val LockPack.authorsString: String
    get() = authors.joinToString(", ")
val LockPack.iconHtml: String
    get() = "<img src=\"${iconFile.relativeTo(rootDir).unixPath}\" alt=\"icon\" style=\"max-height: 128px;\"/>"

data class PackDiff(
    val newPack: LockPack,
    val oldPack: LockPack?
) : KLogging() {
    fun writeChangelog(newMeta: File, oldMeta: File?, docDir: File, generator: ChangelogBuilder) {
        val oldMeta = oldMeta ?: File.createTempFile("empty", "")
        val newPackMetaInfo = writePackMetaInformation(newMeta, newPack)
        val oldPackMetaInfo = readPackMetaInformation(oldMeta)

        logger.debug("reading newMeta: $newMeta, $newPack")
        logger.debug("reading oldMeta: $oldMeta")
        val newEntryMetaInfo = writeEntryMetaInformation(newMeta, newPack)
        val oldEntryMetaInfo = readEntryMetaInformation(oldMeta)

        docDir.mkdirs()
        val changelogFile = newMeta.resolve(Filename.changelog)

        val currentChangelogText = buildString {
            // TODO:  write changelog

            with(generator) {
                this@buildString.writeChangelog(
                    newPack,
                    oldPack,
                    newPackMetaInfo,
                    oldPackMetaInfo,
                    newEntryMetaInfo,
                    oldEntryMetaInfo
                )
            }
        }

        logger.info("writing changelog to $changelogFile")

        // write changelog into changelogFile
        changelogFile.writeText(currentChangelogText)

        oldMeta.mkdirs()

        // copy files to documentation
        changelogFile.copyTo(docDir.resolve(Filename.changelog), overwrite = true)
    }

    companion object : KLogging() {
        private object Filename {
            const val changelog = "changelog.md"
            const val completeChangelog = "complete_changelog.md"
            const val packMeta = "pack.meta.json"
            const val entryMeta = "entry.meta.json"
        }

        fun writeFullChangelog(meta: File, versions: List<String>, docDir: File) {
            // TODO: generate changelogs from scratch every time
            val changelogs = versions.mapNotNull { version ->
                meta.resolve(version).resolve(Filename.changelog).takeIf { it.exists() }?.readText()
            }
            val fullChangelogFile = meta.resolve(Filename.completeChangelog)
            fullChangelogFile.writeText(changelogs.joinToString("\n"))
            fullChangelogFile.copyTo(docDir.resolve(Filename.completeChangelog), overwrite = true)
        }

        private val packMetaSerializer = MapSerializer(String.serializer(), String.serializer())
        private val entryMetaSerializer = MapSerializer(String.serializer(), packMetaSerializer)

        fun writePackMetaInformation(newMeta: File, pack: LockPack): Map<String, String> {
            val reportMap = pack.report().toMap()

            val reportFile = newMeta.resolve(Filename.packMeta)
            val json = json.stringify(packMetaSerializer, reportMap)

            newMeta.mkdirs()
            reportFile.writeText(json)

            return reportMap
        }

        fun readPackMetaInformation(oldMeta: File): Map<String, String> {
            val reportFile = oldMeta.resolve(Filename.packMeta)
            if (!reportFile.exists()) return mapOf()
            return json.parse(packMetaSerializer, reportFile.readText())
        }

        fun writeEntryMetaInformation(newMeta: File, pack: LockPack): Map<String, Map<String, String>> {
            val reportMap = pack.entrySet.sortedBy {
                it.displayName.toLowerCase()
            }.associate { entry ->
                val provider = Providers[entry.provider]

                entry.id.toLowerCase() to provider.reportData(entry)
            }.mapValues { (id, pairs) ->
                pairs.toMap()
            }
            val reportFile = newMeta.resolve(Filename.entryMeta)
            val json = json.stringify(entryMetaSerializer, reportMap)

            newMeta.mkdirs()
            logger.info("writing $reportFile")
            reportFile.writeText(json)

            return reportMap
        }

        fun readEntryMetaInformation(oldMeta: File): Map<String, Map<String, String>> {
            val reportFile = oldMeta.resolve(Filename.entryMeta)
            if (!reportFile.exists()) return mapOf()
            return json.parse(entryMetaSerializer, reportFile.readText())
//            return (json.parseJson(reportFile.readText()) as JsonObject).content.mapValues { (key, value) ->
//                (value as JsonObject).content.mapValues { (key, value) ->
//                    (value as JsonPrimitive).content
//                }
//            }
        }

        private fun changeTable(
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

            oldMetaInfo.filterKeys { key -> !newMetaInfo.containsKey(key) }
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
            )
        }
    }
}