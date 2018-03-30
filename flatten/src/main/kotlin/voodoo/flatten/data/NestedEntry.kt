package voodoo.core.data.nested

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import voodoo.core.curse.DependencyType
import voodoo.core.curse.FileType
import voodoo.core.curse.PackageType
import voodoo.core.data.flat.EntryFeature
import voodoo.core.data.Side
import voodoo.core.data.flat.Entry
import voodoo.core.provider.UpdateChannel
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 * @version 1.0
 */

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class NestedEntry(
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var provider: String = "CURSE",
        var name: String = "",
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
        // INTERNAL //TODO: move into internal object or runtime data objects
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
        var updateChannel: UpdateChannel = UpdateChannel.recommended,
        var template: String = "",
        // NESTED
        var entries: List<NestedEntry> = emptyList()
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
    fun flatten(): List<Entry> {
        flatten("")
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

    private fun flatten(indent: String) {
        val toDelete = mutableListOf<NestedEntry>()
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

            entry.flatten("$indent|  ")
            if (entry.entries.isNotEmpty()) {
                toDelete += entry
            }
            entry.entries.forEach { entries += it }
            entry.entries = listOf()
        }
        entries = entries.filter { !toDelete.contains(it) }
    }
}