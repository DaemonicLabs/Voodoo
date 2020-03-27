package voodoo.data.nested

import mu.KLogging
import voodoo.data.DependencyType
import voodoo.data.OptionalData
import voodoo.data.Side
import voodoo.data.curse.FileID
import voodoo.data.curse.FileType
import voodoo.data.curse.PackageType
import voodoo.data.curse.ProjectID
import voodoo.data.flat.Entry
import voodoo.data.provider.UpdateChannel
import java.io.File
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

data class NestedEntry(
    var provider: String = "",
    var id: String = "",
    var name: String? = null,
    var folder: String = "mods",
    var description: String? = null,
    var optionalData: OptionalData? = null,
    var side: Side = Side.BOTH,
    var websiteUrl: String = "",
//    var provides: MutableMap<DependencyType, List<String>> = mutableMapOf(),
//    @Deprecated("waiting to be redone")
    var dependencies: MutableMap<DependencyType, List<String>> = mutableMapOf(),
//    @Deprecated("waiting to be redone")
    var replaceDependencies: Map<ProjectID, ProjectID> = mapOf(),
    var packageType: PackageType = PackageType.MOD,
    var transient: Boolean = false, // this entry got added as dependency for something else
    var version: String = "", // TODO: use regex only ?
    var fileName: String? = null,
    var fileNameRegex: String? = null,
    var validMcVersions: Set<String> = setOf(),
    var enabled: Boolean = true,
    //  CURSE
    var curseReleaseTypes: Set<FileType> = setOf(FileType.Release, FileType.Beta),
    var curseProjectID: ProjectID = ProjectID.INVALID,
    var curseFileID: FileID = FileID.INVALID,
    //  DIRECT
    var url: String = "",
    var useUrlTxt: Boolean = true,
    //  JENKINS
    var jenkinsUrl: String = "",
    var job: String = "",
    var buildNumber: Int = -1,
    //  LOCAL
    var fileSrc: String = "",
    //  UPDATE-JSON
    var updateJson: String = "",
    var updateChannel: UpdateChannel = UpdateChannel.RECOMMENDED,
    var template: String = "",
    //  NESTED
    var entries: List<NestedEntry> = emptyList()
) {
    companion object : KLogging() {
        val DEFAULT = NestedEntry()
    }

    suspend fun flatten(parentFile: File? = null): List<Entry> {
        flatten("", parentFile)

        // remove duplicate entries
        val ids = mutableSetOf<String>()
        entries.forEach {
            if (it.id in ids) {
                entries -= it
            } else {
                ids += it.id
            }
        }
        return this.entries.asSequence().filter { it.enabled }.map { entry ->
            Entry(
                entry.provider,
                id = entry.id,
                name = entry.name,
                folder = entry.folder,
                description = entry.description,
                optionalData = entry.optionalData,
                side = entry.side,
                websiteUrl = entry.websiteUrl,
                dependencies = entry.dependencies,
                replaceDependencies = entry.replaceDependencies,
                //                optional = it.optional,
                packageType = entry.packageType,
                transient = entry.transient,
                version = entry.version,
                fileName = entry.fileName,
                //                fileNameRegex = it.fileNameRegex,
                validMcVersions = entry.validMcVersions,
                // CURSE
                curseReleaseTypes = entry.curseReleaseTypes,
                curseProjectID = entry.curseProjectID,
                curseFileID = entry.curseFileID,
                // DIRECT
                url = entry.url,
                useUrlTxt = entry.useUrlTxt,
                // JENKINS
                jenkinsUrl = entry.jenkinsUrl,
                job = entry.job,
                buildNumber = entry.buildNumber,
                // LOCAL
                fileSrc = entry.fileSrc,
                // UPDATE JSON
                updateJson = entry.updateJson,
                updateChannel = entry.updateChannel,
                template = entry.template
            ).apply {
                entry.fileNameRegex?.let {
                    fileNameRegex = it
                }
            }
        }.toList()
    }

    private suspend fun flatten(indent: String, parentFile: File? = null) {
        val toDelete = mutableListOf<NestedEntry>()

        entries.forEach { entry ->

            // set feature of entry from `this` or DEFAULT

//            if ((entry.provider == DEFAULT.provider || entry.provider.isBlank()) && provider != DEFAULT.provider) entry.provider = provider
//            if (entry.id == DEFAULT.id && id != DEFAULT.id) entry.id = id
//            if (entry.rootFolder == DEFAULT.rootFolder && rootFolder != DEFAULT.rootFolder) entry.rootFolder = rootFolder
//            if (entry.comment == DEFAULT.comment && comment != DEFAULT.comment) entry.comment = comment
//            if (entry.description == DEFAULT.description && description != DEFAULT.description) entry.description = description
//            if (entry.feature == DEFAULT.feature && feature != DEFAULT.feature) entry.feature = feature
//            if (entry.side == DEFAULT.side && side != DEFAULT.side) entry.side = side
//            if (entry.websiteUrl == DEFAULT.websiteUrl && websiteUrl != DEFAULT.websiteUrl) entry.websiteUrl = websiteUrl
//            if (entry.provides == DEFAULT.provides && provides != DEFAULT.provides) entry.provides = provides
//            if (entry.dependencies == DEFAULT.dependencies && dependencies != DEFAULT.dependencies) entry.dependencies = dependencies
//            if (entry.packageType == DEFAULT.packageType && packageType != DEFAULT.packageType) entry.packageType = packageType
//            if (entry.transient == DEFAULT.transient && transient != DEFAULT.transient) entry.transient = transient
//            if (entry.version == DEFAULT.version && version != DEFAULT.version) entry.version = version
//            if (entry.fileName == DEFAULT.fileName && fileName != DEFAULT.fileName) entry.fileName = fileName
//            if (entry.fileNameRegex == DEFAULT.fileNameRegex && fileNameRegex != DEFAULT.fileNameRegex) entry.fileNameRegex = fileNameRegex
//            if (entry.validMcVersions == DEFAULT.validMcVersions && validMcVersions != DEFAULT.validMcVersions) entry.validMcVersions = validMcVersions
//            //  CURSE
//            if (entry.curseMetaUrl == DEFAULT.curseMetaUrl && curseMetaUrl != DEFAULT.curseMetaUrl) entry.curseMetaUrl = curseMetaUrl
//            if (entry.curseReleaseTypes == DEFAULT.curseReleaseTypes && curseReleaseTypes != DEFAULT.curseReleaseTypes) entry.curseReleaseTypes = curseReleaseTypes
//            if (entry.url == DEFAULT.url && url != DEFAULT.url) entry.url = url
//            if (entry.useUrlTxt == DEFAULT.useUrlTxt && useUrlTxt != DEFAULT.useUrlTxt) entry.useUrlTxt = useUrlTxt
//            //  JENKINS
//            if (entry.jenkinsUrl == DEFAULT.jenkinsUrl && jenkinsUrl != DEFAULT.jenkinsUrl) entry.jenkinsUrl = jenkinsUrl
//            if (entry.job == DEFAULT.job && job != DEFAULT.job) entry.job = job
//            if (entry.buildNumber == DEFAULT.buildNumber && buildNumber != DEFAULT.buildNumber) entry.buildNumber = buildNumber
//            //  LOCAL
//            if (entry.fileSrc == DEFAULT.fileSrc && fileSrc != DEFAULT.fileSrc) entry.fileSrc = fileSrc
//            //  UPDATE
//            if (entry.updateJson == DEFAULT.updateJson && updateJson != DEFAULT.updateJson) entry.updateJson = updateJson
//            if (entry.updateChannel == DEFAULT.updateChannel && updateChannel != DEFAULT.updateChannel) entry.updateChannel = updateChannel
//            if (entry.template == DEFAULT.template && template != DEFAULT.template) entry.template = template

            for (prop in NestedEntry::class.memberProperties) {
                if (prop is KMutableProperty<*>) {
                    val otherValue = prop.get(entry)
                    val thisValue = prop.get(this)
                    val defaultValue = prop.get(DEFAULT)
                    if (otherValue == defaultValue && thisValue != defaultValue) {
                        if (prop.name != "entries" && prop.name != "template") {
                            // clone maps
                            when (thisValue) {
                                is MutableMap<*, *> -> {
                                    val map = thisValue.toMutableMap()
                                    // copy lists
                                    map.forEach { k, v ->
                                        if (v is List<*>) {
                                            map[k] = v.toList()
                                        }
                                    }
                                    prop.setter.call(entry, map)
                                }
                                is Set<*> -> prop.setter.call(entry, thisValue.toSet())
                                else ->
                                    prop.setter.call(entry, thisValue)
                            }
                        }
                    }
                }
            }

            entry.flatten("$indent|  ", parentFile)
            if (entry.entries.isNotEmpty() || entry.id.isBlank()) {
                toDelete += entry
            }

            entry.entries.forEach { entries += it }
            entry.entries = emptyList()
        }
        entries = entries.filter { !toDelete.contains(it) }
        entries.forEach { entry ->
            if (entry.id.isEmpty()) {
                logger.error { entry }
                throw IllegalStateException("entries with blank id must not persist")
            }
        }
    }
}