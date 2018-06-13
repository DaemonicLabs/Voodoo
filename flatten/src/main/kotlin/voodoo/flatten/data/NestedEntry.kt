package voodoo.flatten.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import voodoo.data.curse.DependencyType
import voodoo.data.curse.FileType
import voodoo.data.curse.PackageType
import voodoo.data.Side
import voodoo.data.flat.Entry
import voodoo.data.flat.EntryFeature
import voodoo.data.provider.UpdateChannel
import voodoo.util.json
import voodoo.util.readJson
import voodoo.util.readYaml
import java.io.File
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class NestedEntry(
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var provider: String = "CURSE",
        var name: String = "",
        var folder: String = "mods",
        var comment: String = "",
        var description: String = "",
        var feature: EntryFeature? = null,
        var side: Side = Side.BOTH,
        var websiteUrl: String = "",
        var provides: MutableMap<DependencyType, List<String>> = mutableMapOf(),
        var dependencies: MutableMap<DependencyType, List<String>> = mutableMapOf(),
//        @JsonInclude(JsonInclude.Include.ALWAYS)
//        var optional: Boolean = feature != null,
        var packageType: PackageType = PackageType.MOD,
        // INTERNAL //TODO: move into internal object or runtime data objects ?
//        @JsonIgnore
//        val internal: EntryInternal = EntryInternal(),
        var transient: Boolean = false, // this entry got added as dependency for something else
        var version: String = "", //TODO: use regex only ?
        var validMcVersions: List<String> = emptyList(),
        var curseReleaseTypes: Set<FileType> = setOf(FileType.RELEASE, FileType.BETA),
        var curseFileNameRegex: String = ".*(?<!-deobf\\.jar)\$",
        var curseOptionalDependencies: Boolean = false,
        // DIRECT
        var url: String = "",
        var urlTxt: Boolean = true,
        var fileName: String? = null,
        //JENKINS
        var jenkinsUrl: String = "",
        var job: String = "",
        var buildNumber: Int = -1,
        var jenkinsFileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$",
        // LOCAL
        var fileSrc: String = "",
        // UPDATE-JSON
        var updateJson: String = "",
        var updateChannel: UpdateChannel = UpdateChannel.RECOMMENDED,
        var template: String = "",
        // NESTED
        var entries: List<NestedEntry> = emptyList(),
        var include: String? = null
) {
    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(stringValue: String): NestedEntry {
            return NestedEntry().apply { name = stringValue }
        }

        private val default = NestedEntry()
    }

    //        override fun toString(): String {
//                return if(entries.isEmpty())
//                        super.toString()
//                else
//                        "NestedEntry(super=${super.toString()}, entries=$entries)"
//        }
    fun flatten(parentFile: File): List<Entry> {
        flatten("", parentFile)
        return this.entries.map { it ->
            Entry().apply {
                for (prop in NestedEntry::class.memberProperties) {
                    if (prop is KMutableProperty<*>) {
                        val value = prop.get(it)
                        val otherPop = (Entry::class.memberProperties.find { it.name == prop.name })
                                as? KMutableProperty<*>
                                ?: continue
                        otherPop.setter.call(this, value)
                    }
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
                    val defaultValue = prop.get(default)

                    if (thisValue == defaultValue) {
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
            for (prop in NestedEntry::class.memberProperties) {
                if (prop is KMutableProperty<*>) {
                    val otherValue = prop.get(entry)
                    val thisValue = prop.get(this)
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