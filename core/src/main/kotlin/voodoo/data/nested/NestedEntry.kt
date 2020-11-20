package voodoo.data.nested

import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KotlinLogging
import voodoo.data.DependencyType
import voodoo.data.OptionalData
import voodoo.data.Side
import voodoo.data.components.*
import voodoo.data.curse.PackageType
import voodoo.data.flat.Entry
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties

private val logger = KotlinLogging.logger {}

@Serializable
sealed class NestedEntry(
    override var name: String? = null,
    override var folder: String? = null,
    override var description: String? = null,
    override var optionalData: OptionalData? = null,
    override var side: Side = Side.BOTH,
    override var websiteUrl: String = "",
    override var dependencies: MutableMap<String, DependencyType> = mutableMapOf(),
//    override var replaceDependencies: Map<ProjectID, ProjectID> = mapOf(), // TODO: replace with Map<String, ...>
    override var packageType: PackageType = PackageType.MOD,
    override var transient: Boolean = false, // this entry got added as dependency for something else
    override var version: String = "", // TODO: use regex only ?
    override var fileName: String? = null,
    override var fileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$",
    override var validMcVersions: Set<String> = setOf(),
    override var invalidMcVersions: Set<String> = setOf(),
    override var enabled: Boolean = true,
) : Common {
    abstract var entries: Map<String, NestedEntry>
    abstract var nodeName: String?
    abstract val provider: String

    @Serializable
    @SerialName("common")
    data class Common(
        @Transient override var nodeName: String? = null,
        override var entries: Map<String, NestedEntry> = emptyMap()
    ) : NestedEntry() {
        @Transient override val provider = "common"

    }

    @Serializable
    @SerialName("curse")
    data class Curse(
        @Transient override var nodeName: String? = null,
        @JsonSchema.StringEnum(["replace_with_curseforge_projects"])
        var projectName: String? = null,
        @SerialName("curseProperties")
        val curse: CurseComponent = CurseComponent(),
        override var entries: Map<String, NestedEntry> = emptyMap()
    ) : NestedEntry(), CurseMutable by curse {
        @Transient override val provider = "curse"

        companion object: NestedEntryProvider<Curse> {
            override fun create() = Curse()
        }
    }

    @Serializable
    @SerialName("direct")
    data class Direct(
        @Transient override var nodeName: String? = null,
        @SerialName("directProperties")
        val direct: DirectComponent = DirectComponent(),
        override var entries: Map<String, NestedEntry> = emptyMap()
    ) : NestedEntry(), DirectMutable by direct {
        @Transient override val provider = "direct"

        companion object: NestedEntryProvider<Direct> {
            override fun create() = Direct()
        }
    }

    @Serializable
    @SerialName("jenkins")
    data class Jenkins(
        @Transient override var nodeName: String? = null,
        @SerialName("jenkinsProperties")
        val jenkins: JenkinsComponent = JenkinsComponent(),
        override var entries: Map<String, NestedEntry> = emptyMap()
    ) : NestedEntry(), JenkinsMutable by jenkins {
        @Transient override val provider = "jenkins"

        companion object: NestedEntryProvider<Jenkins> {
            override fun create() = Jenkins()
        }
    }

    @Serializable
    @SerialName("local")
    data class Local(
        @Transient override var nodeName: String? = null,
        @SerialName("localProperties")
        val local: LocalComponent = LocalComponent(),
        override var entries: Map<String, NestedEntry> = emptyMap()
    ) : NestedEntry(), LocalMutable by local {
        @Transient override val provider = "local"
        companion object: NestedEntryProvider<Local> {
            override fun create() = Local()
        }
    }

    @Serializable
    @SerialName("noop")
    data class Noop(
        @Transient override var nodeName: String? = null,
        override var entries: Map<String, NestedEntry> = emptyMap()
    ): NestedEntry() {
        @Transient override val provider = "noop"
        companion object: NestedEntryProvider<Noop> {
            override fun create() = Noop()
        }
    }

//    private val debugIdentifier: String
//        get() = nodeName ?: toString()

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private fun toCommonComponent(id: String) = CommonComponent(
        id = id,
        name = name,
        folder = folder,
        description = description,
        optionalData = optionalData?.copy(),
        side = side,
        websiteUrl = websiteUrl,
        dependencies = dependencies,
        packageType = packageType,
        transient = transient,
        version = version,
        fileName = fileName,
        fileNameRegex = fileNameRegex,
        validMcVersions = validMcVersions,
        invalidMcVersions = invalidMcVersions,
        enabled = enabled
    )

    fun flatten(id: String = "root"): List<Entry> {
        flatten("", id)

        // no longer of use because duplicate ids cannot happen
//        // remove duplicate entries
//        val ids = mutableSetOf<String>()
//
//        entries.forEach { entry ->
//            if (entry.id in ids) {
//                entries -= entry
//            } else {
//                ids += entry.id
//            }
//        }
        // copy entries

        logger.trace { "flattened entries: ${entries}" }

        val entryList = entries.filterValues { it.enabled }.map { (entryId, entry) ->
//            if(!entry.enabled) return null

            logger.trace { "converting to Entry: id=$entryId nestedEntry=$entry" }
            with(entry) {
                when (this) {
                    is Common -> Entry.Common(
                        common = toCommonComponent(entryId)
                    )
                    is Curse -> Entry.Curse(
                        common = toCommonComponent(entryId),
                        curse = curse.copy()
                    )
                    is Direct -> Entry.Direct(
                        common = toCommonComponent(entryId),
                        direct = direct.copy()
                    )
                    is Jenkins -> Entry.Jenkins(
                        common = toCommonComponent(entryId),
                        jenkins = jenkins.copy()
                    )
                    is Local -> Entry.Local(
                        common = toCommonComponent(entryId),
                        local = local.copy()
                    )
                    is Noop -> Entry.Noop(
                        common = toCommonComponent(entryId)
                    )
                }
            }
        }.toList()

        logger.trace { "entryList: $entryList"}

        return entryList
    }

    private fun flatten(indent: String, parentId: String) {
//        val toDelete = mutableListOf<String>()

        entries.forEach { (id, entry) ->
            logger.debug { "$indent pre_flatten: ${entry}" }

            // set feature of entry from `this` or DEFAULT

            logger.debug { "$indent copying fields of '${parentId}' -> '${id}'" }

            // TODO: avoid creating and throwing away objects for defaults
            mergeProperties<voodoo.data.components.Common>(this, entry, Common())

            when {
                entry is Curse && this is Curse -> {
                    mergeProperties<CurseMutable>(this, entry, Curse())
                }
                entry is Direct && this is Direct -> {
                    mergeProperties<DirectMutable>(this, entry, Direct())
                }
                entry is Jenkins && this is Jenkins -> {
                    mergeProperties<JenkinsMutable>(this, entry, Jenkins())
                }
                entry is Local && this is Local -> {
                    mergeProperties<LocalMutable>(this, entry, Local())
                }
            }

            logger.trace { "$indent copying to parent: ${entry.entries.keys}" }
            this.entries += entry.entries
            entry.flatten("$indent|  ", id)
        }
        entries = entries.filter { (id, entry) ->
            if (entry.entries.isNotEmpty() || id.isBlank()) {
                logger.trace { "dropping group: $id" }
                false
            } else {
                true
            }
        }
//        entries = entries.filterKeys { !toDelete.contains(it) }
        entries.forEach { (id, entry) ->
            if (id.isBlank()) {
                logger.error { entry }
                error("entries with blank id must not persist")
            }
        }
        logger.trace { "$indent post_flatten: entries: ${entries.keys}" }
    }
}

private inline fun <reified T : Any> mergeProperties(a: T, other: T, default: T) {
    for (prop in T::class.memberProperties) {
        if (prop is KMutableProperty<*>) {
            val otherValue = prop.get(other)
            val thisValue = prop.get(a)
            val defaultValue = prop.get(default)
            if (otherValue == defaultValue && thisValue != defaultValue) {
                if (prop.name != "entries") {
                    // clone maps
                    when (thisValue) {
                        is MutableMap<*, *> -> {
                            val map = thisValue.toMutableMap()
                            // copy lists
                            map.forEach { (k, v) ->
                                if (v is List<*>) {
                                    map[k] = v.toList()
                                }
                            }
                            prop.setter.call(other, map)
                        }
                        is Set<*> -> prop.setter.call(other, thisValue.toSet())
                        else -> {
                            if (thisValue != null) {
                                val thisClass = thisValue::class
                                val copyFuncRef =
                                    thisClass.functions.find { it.name == "copy" && it.parameters.isEmpty() }
                                if (copyFuncRef != null) {
                                    logger.debug("copy found for $thisClass")
                                    prop.setter.call(other, copyFuncRef.call(thisValue))
                                } else {
                                    prop.setter.call(other, thisValue)
                                }
                            } else {
                                prop.setter.call(other, null)
                            }
                        }
                    }
                }
            }
        }
    }
}
