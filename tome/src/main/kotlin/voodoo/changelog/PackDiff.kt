package voodoo.changelog

import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.markdownTable
import voodoo.provider.Providers
import voodoo.util.json
import voodoo.util.unixPath
import java.io.File
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import voodoo.data.EntryReportData
import voodoo.data.PackReportData

val LockPack.authorsString: String
    get() = authors.joinToString(", ")
val LockPack.iconHtml: String
    get() = "<img src=\"${iconFile.relativeTo(lockBaseFolder).unixPath}\" alt=\"icon\" style=\"max-height: 128px;\"/>"

data class PackDiff(
    val newPack: LockPack,
    val oldPack: LockPack?
) : KLogging() {
    companion object : KLogging() {
        private object Filename {
            const val packMeta = "pack.meta.json"
            const val entryMeta = "entry.meta.json"
        }

        fun writeChangelog(oldMeta: File?, newMeta: File, tmpChangelogFile: File, docDir: File, generator: ChangelogBuilder): File {
            val oldMeta = oldMeta ?: File.createTempFile("empty", "")
            val oldPackMetaInfo = readPackMetaInformation(oldMeta)
            val newPackMetaInfo = readPackMetaInformation(newMeta)

            logger.debug("reading newMeta: $newMeta")
            logger.debug("reading oldMeta: $oldMeta")
            val newEntryMetaInfo = readEntryMetaInformation(newMeta)
            val oldEntryMetaInfo = readEntryMetaInformation(oldMeta)

            docDir.mkdirs()
//            val changelogFile = workingDir.resolve(generator.filename)

            val currentChangelogText = buildString {
                // TODO:  write changelog

                with(generator) {
                    this@buildString.writeChangelog(
                        ChangelogStepData(
                            newPackMetaInfo = newPackMetaInfo,
                            oldPackMetaInfo = oldPackMetaInfo,
                            newEntryMetaInfo = newEntryMetaInfo,
                            oldEntryMetaInfo = oldEntryMetaInfo
                        )
                    )
                }
            }

            logger.info("writing changelog to $tmpChangelogFile")

            // write changelog into changelogFile
            tmpChangelogFile.writeText(currentChangelogText)

            // copy files to documentation
            tmpChangelogFile.copyTo(docDir.resolve(generator.filename), overwrite = true)

            return tmpChangelogFile
        }

        fun writeFullChangelog(
            steps: List<Pair<File?, File>>,
            docDir: File,
            tmpChangelogFile: File,
            generator: ChangelogBuilder
        ) {
            val changelogSteps = steps.map { (oldMeta, newMeta) ->
                val oldMeta = oldMeta ?: File.createTempFile("empty", "")
                val oldPackMetaInfo = readPackMetaInformation(oldMeta)
                val newPackMetaInfo = readPackMetaInformation(newMeta)

                logger.debug("reading newMeta: $newMeta")
                logger.debug("reading oldMeta: $oldMeta")
                val newEntryMetaInfo = readEntryMetaInformation(newMeta)
                val oldEntryMetaInfo = readEntryMetaInformation(oldMeta)
                ChangelogStepData(
                    newPackMetaInfo = newPackMetaInfo,
                    oldPackMetaInfo = oldPackMetaInfo,
                    newEntryMetaInfo = newEntryMetaInfo,
                    oldEntryMetaInfo = oldEntryMetaInfo
                )
            }

            val fullChangelogText = buildString {
                // TODO:  write changelog

                with(generator) {
                    this@buildString.writeFullChangelog(
                        steps = changelogSteps
                    )
                }
            }

            logger.info("writing changelog to $tmpChangelogFile")

            // write changelog into changelogFile
            tmpChangelogFile.writeText(fullChangelogText)

            // copy files to documentation
            tmpChangelogFile.copyTo(docDir.resolve(generator.fullFilename), overwrite = true)

        }

        private val packMetaSerializer = MapSerializer(String.serializer(), String.serializer())
        private val entryMetaSerializer = MapSerializer(String.serializer(), packMetaSerializer)

        fun writePackMetaInformation(newMeta: File, pack: LockPack): Map<PackReportData, String> {
            val reportMap = pack.report()

            val reportDataForJson = reportMap.mapKeys { (reportData, _) ->
                reportData.key
            }

            val reportFile = newMeta.resolve(Filename.packMeta)
            val json = json.encodeToString(packMetaSerializer, reportDataForJson)

            newMeta.mkdirs()
            reportFile.writeText(json)

            return reportMap
        }

        fun readPackMetaInformation(oldMeta: File): Map<PackReportData, String> {
            val reportFile = oldMeta.resolve(Filename.packMeta)
            if (!reportFile.exists()) return mapOf()
            return json.decodeFromString(packMetaSerializer, reportFile.readText()).mapKeys { (key, _) ->
                PackReportData.getByKey(key)!!
            }
        }

        fun writeEntryMetaInformation(newMeta: File, pack: LockPack): Map<String, MutableMap<EntryReportData, String>> {
            val reportMap = pack.entries.sortedBy {
                it.displayName.toLowerCase()
            }.associate { entry ->
                val provider = Providers[entry.provider]

                entry.id.toLowerCase() to provider.reportData(entry)
            }
            val reportDataForJson = reportMap.mapValues { (_, entryData) ->
                entryData.mapKeys { (reportData, _) ->
                    reportData.key
                }
            }
            val reportFile = newMeta.resolve(Filename.entryMeta)
            val json = json.encodeToString(entryMetaSerializer, reportDataForJson)

            newMeta.mkdirs()
            logger.info("writing $reportFile")
            reportFile.writeText(json)

            return reportMap
        }

        fun readEntryMetaInformation(oldMeta: File): Map<String, Map<EntryReportData, String>> {
            val reportFile = oldMeta.resolve(Filename.entryMeta)
            if (!reportFile.exists()) return mapOf()
            return json.decodeFromString(entryMetaSerializer, reportFile.readText()).mapValues {(_, entryData) ->
                entryData.mapKeys { (key, _) ->
                    EntryReportData.getByKey(key)!!
                }
            }
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