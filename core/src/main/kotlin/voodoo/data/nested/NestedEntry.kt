package voodoo.data.nested

import mu.KLogging
import voodoo.data.components.*
import voodoo.data.flat.Entry
import voodoo.provider.*
import java.io.File
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

sealed class NestedEntry(
    open var entries: List<NestedEntry> = emptyList()
) : CommonMutable {
    data class Common(
        val common: CommonComponent = CommonComponent(),
        override var entries: List<NestedEntry> = emptyList()
    ) : NestedEntry(entries), CommonMutable by common {
        init {
            provider = ""
        }
    }

    data class Curse(
        val common: CommonComponent = CommonComponent(),
        val curse: CurseComponent = CurseComponent(),
        override var entries: List<NestedEntry> = emptyList()
    ) : NestedEntry(entries), CommonMutable by common, CurseMutable by curse {
        init {
            provider = CurseProvider.id
        }
    }

    data class Direct(
        val common: CommonComponent = CommonComponent(),
        val direct: DirectComponent = DirectComponent(),
        override var entries: List<NestedEntry> = emptyList()
    ) : NestedEntry(entries), CommonMutable by common, DirectMutable by direct {
        init {
            provider = DirectProvider.id
        }
    }

    data class Jenkins(
        val common: CommonComponent = CommonComponent(),
        val jenkins: JenkinsComponent = JenkinsComponent(),
        override var entries: List<NestedEntry> = emptyList()
    ) : NestedEntry(entries), CommonMutable by common, JenkinsMutable by jenkins {
        init {
            provider = JenkinsProvider.id
        }
    }

    data class Local(
        val common: CommonComponent = CommonComponent(),
        val local: LocalComponent = LocalComponent(),
        override var entries: List<NestedEntry> = emptyList()
    ) : NestedEntry(entries), CommonMutable by common, LocalMutable by local {
        init {
            provider = LocalProvider.id
        }
    }

    data class UpdateJson(
        val common: CommonComponent = CommonComponent(),
        val _updateJson: UpdateJsonComponent = UpdateJsonComponent(),
        override var entries: List<NestedEntry> = emptyList()
    ) : NestedEntry(entries), CommonMutable by common, UpdateJsonMutable by _updateJson {
        init {
            provider = UpdateJsonProvider.id
        }
    }

    companion object : KLogging() {
//        val DEFAULT = NestedEntry()
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
            when(entry) {
                is Common -> Entry.Common(
                    common = entry.common.copy()
                )
                is Curse -> Entry.Curse(
                    common = entry.common.copy(),
                    curse = entry.curse.copy()
                )
                is Direct ->  Entry.Direct(
                    common = entry.common.copy(),
                    direct = entry.direct.copy()
                )
                is Jenkins -> Entry.Jenkins(
                    common = entry.common.copy(),
                    jenkins = entry.jenkins.copy()
                )
                is Local -> Entry.Local(
                    common = entry.common.copy(),
                    local = entry.local.copy()
                )
                is UpdateJson ->  Entry.UpdateJson(
                    common = entry.common.copy(),
                    _updateJson = entry._updateJson.copy()
                )
            }


//            Entry(
//                entry.provider,
//                id = entry.id,
//                name = entry.name,
//                folder = entry.folder,
//                description = entry.description,
//                optionalData = entry.optionalData,
//                side = entry.side,
//                websiteUrl = entry.websiteUrl,
//                dependencies = entry.dependencies,
//                replaceDependencies = entry.replaceDependencies,
//                //                optional = it.optional,
//                packageType = entry.packageType,
//                transient = entry.transient,
//                version = entry.version,
//                fileName = entry.fileName,
//                //fileNameRegex = entry.fileNameRegex ?: commonfileNameRegex, // should use the default if this value is null
//                validMcVersions = entry.validMcVersions
//            ).apply {
//                when(entry) {
//                    // TODO: just copy the components over...
//                    is CurseMutable -> {
//                        curseReleaseTypes = entry.releaseTypes
//                        curseProjectID = entry.projectID
//                        curseFileID = entry.fileID
//                    }
//                    is DirectMutable -> {
//                        url = entry.url
//                        useUrlTxt = entry.useUrlTxt
//                    }
//                    is JenkinsMutable -> {
//                        jenkinsUrl = entry.jenkinsUrl
//                        job = entry.job
//                        buildNumber = entry.buildNumber
//                    }
//                    is LocalMutable -> {
//                        fileSrc = entry.fileSrc
//                    }
//                    is UpdateJsonMutable -> {
//                        updateJson = entry.updateJson
//                        updateChannel = entry.updateChannel
//                        template = entry.template
//                    }
//                }
//
//                // should use the default if this value is null
//                entry.fileNameRegex?.let {
//                    fileNameRegex = it
//                }
//            }
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

            // TODO: avoid creating and throwing away objects for defaults
            mergeProperties<CommonMutable>(this, entry, Common())

            when {
                entry is Curse && this is Curse -> {
                    mergeProperties<CurseMutable>(this, entry, Curse())
                }
                entry is Direct && this is Direct -> {
                    mergeProperties<DirectMutable>(this, entry, Direct())
                }
                entry is Jenkins && this is Jenkins ->{
                    mergeProperties<JenkinsMutable>(this, entry, Jenkins())
                }
                entry is Local && this is Local ->{
                    mergeProperties<LocalMutable>(this, entry, Local())
                }
                entry is UpdateJson && this is UpdateJson -> {
                    mergeProperties<UpdateJsonMutable>(this, entry, UpdateJson())
                }
            }

//            for (prop in clazz.memberProperties) {
//                if (prop is KMutableProperty<*>) {
//                    val otherValue = prop.get(entry)
//                    val thisValue = prop.get(this)
//                    val defaultValue = prop.get(DEFAULT)
//                    if (otherValue == defaultValue && thisValue != defaultValue) {
//                        if (prop.name != "entries" && prop.name != "template") {
//                            // clone maps
//                            when (thisValue) {
//                                is MutableMap<*, *> -> {
//                                    val map = thisValue.toMutableMap()
//                                    // copy lists
//                                    map.forEach { k, v ->
//                                        if (v is List<*>) {
//                                            map[k] = v.toList()
//                                        }
//                                    }
//                                    prop.setter.call(entry, map)
//                                }
//                                is Set<*> -> prop.setter.call(entry, thisValue.toSet())
//                                else ->
//                                    prop.setter.call(entry, thisValue)
//                            }
//                        }
//                    }
//                }
//            }

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

private inline fun <reified T: Any> mergeProperties(a: T, b: T, default: T) {
    for (prop in T::class.memberProperties) {
        if (prop is KMutableProperty<*>) {
            val otherValue = prop.get(b)
            val thisValue = prop.get(a)
            val defaultValue = prop.get(default)
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
                            prop.setter.call(b, map)
                        }
                        is Set<*> -> prop.setter.call(b, thisValue.toSet())
                        else ->
                            prop.setter.call(b, thisValue)
                    }
                }
            }
        }
    }
}
