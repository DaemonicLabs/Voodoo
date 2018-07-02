package voodoo.data.nested

import com.fasterxml.jackson.annotation.JsonCreator
import mu.KLogging
import voodoo.data.Side
import voodoo.data.curse.CurseConstancts
import voodoo.data.curse.DependencyType
import voodoo.data.curse.FileType
import voodoo.data.curse.PackageType
import voodoo.data.flat.Entry
import voodoo.data.flat.EntryFeature
import voodoo.data.provider.UpdateChannel
import voodoo.provider.Provider
import voodoo.util.readYaml
import java.io.File
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

//@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class NestedEntry(
        //@JsonInclude(JsonInclude.Include.ALWAYS)
        var provider: String = "",
        var name: String = "",
        var folder: String = "mods",
        var comment: String = "",
        var description: String = "",
        var feature: EntryFeature? = null,
        var side: Side = Side.BOTH,
        var websiteUrl: String = "",
        var provides: MutableMap<DependencyType, List<String>> = mutableMapOf(),
        var dependencies: MutableMap<DependencyType, List<String>> = mutableMapOf(),
        var packageType: PackageType = PackageType.MOD,
        var transient: Boolean = false, // this entry got added as dependency for something else
        var version: String = "", //TODO: use regex only ?
        var fileName: String? = null,
        var fileNameRegex: String? = null,
        var validMcVersions: Set<String> = setOf(),
        //  CURSE
        var curseMetaUrl: String = CurseConstancts.PROXY_URL,
        var curseReleaseTypes: Set<FileType> = setOf(FileType.RELEASE, FileType.BETA),
        var curseOptionalDependencies: Boolean = false,
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
        var entries: List<NestedEntry> = emptyList(),
        var include: String? = null
) {
    companion object : KLogging() {
        @JvmStatic
        @JsonCreator
        fun fromString(stringValue: String): NestedEntry {
            logger
            return NestedEntry(provider = Provider.CURSE.name).apply { name = stringValue }
        }
        
        val DEFAULT = NestedEntry()
    }

    fun flatten(parentFile: File): List<Entry> {
        flatten("", parentFile)
        return this.entries.map { it ->
            Entry(it.provider,
                    name = it.name,
                    folder = it.folder,
                    comment = it.comment,
                    description = it.description,
                    feature = it.feature,
                    side = it.side,
                    websiteUrl = it.websiteUrl,
                    dependencies = it.dependencies,
//                optional = it.optional,
                    packageType = it.packageType,
                    transient = it.transient,
                    version = it.version,
                    fileName = it.fileName,
//                fileNameRegex = it.fileNameRegex,
                    validMcVersions = it.validMcVersions,
                    // CURSE
                    curseMetaUrl = it.curseMetaUrl,
                    curseReleaseTypes = it.curseReleaseTypes,
                    curseOptionalDependencies = it.curseOptionalDependencies,
                    // DIRECT
                    url = it.url,
                    useUrlTxt = it.useUrlTxt,// JENKINS
                    jenkinsUrl = it.jenkinsUrl,
                    job = it.job,
                    buildNumber = it.buildNumber,
                    // LOCAL
                    fileSrc = it.fileSrc,
                    // UPDATE JSON
                    updateJson = it.updateJson,
                    updateChannel = it.updateChannel,
                    template = it.template
            ).apply {
                it.fileNameRegex?.let {
                    fileNameRegex = it
                }
            }
        }
    }

    private fun flatten(indent: String, parentFile: File) {
        var parent = parentFile
        val toDelete = mutableListOf<NestedEntry>()
        include?.let {
            println("loading $include")
            val includeFile = parentFile.resolve(it)
            val includeEntry = includeFile.readYaml<NestedEntry>()


            for (prop in NestedEntry::class.memberProperties) {
                if (prop is KMutableProperty<*>) {
                    val includeValue = prop.get(includeEntry)
                    val thisValue = prop.get(this)
                    val DEFAULTValue = prop.get(DEFAULT)

                    if (thisValue == DEFAULTValue) {
                        println("setting ${prop.name}")
                        prop.setter.call(this, includeValue)
                    }
                }
            }
            parent = includeFile.parentFile
            println("loaded $includeFile")
            include = null
        }

        entries.forEach { entry ->

            // set properties of entry from `this` or DEFAULT

//            if ((entry.provider == DEFAULT.provider || entry.provider.isBlank()) && provider != DEFAULT.provider) entry.provider = provider
//            if (entry.name == DEFAULT.name && name != DEFAULT.name) entry.name = name
//            if (entry.folder == DEFAULT.folder && folder != DEFAULT.folder) entry.folder = folder
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
//            if (entry.curseOptionalDependencies == DEFAULT.curseOptionalDependencies && curseOptionalDependencies != DEFAULT.curseOptionalDependencies) entry.curseOptionalDependencies = curseOptionalDependencies //  DIRECT
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

            entry.flatten("$indent|  ", parent)
            if (entry.entries.isNotEmpty()) {
                toDelete += entry
            }
            entry.entries.forEach { entries += it }
            entry.entries = listOf()
        }
        entries = entries.filter { !toDelete.contains(it) }
    }
}