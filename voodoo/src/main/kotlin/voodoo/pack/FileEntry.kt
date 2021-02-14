package voodoo.pack

import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import voodoo.config.Autocompletions
import voodoo.data.Side
import voodoo.data.components.*
import voodoo.data.curse.FileID
import voodoo.data.curse.FileType
import voodoo.data.curse.PackageType
import voodoo.data.curse.ProjectID
import voodoo.data.flat.FlatEntry

@Serializable
sealed class FileEntry(
) {
    companion object {
        private val logger = KotlinLogging.logger{}
    }

    interface Common {
        val applyOverrides: List<String>
        var id: String?
        var name: String?
        var folder: String?
        var description: String?
        var optional: Optional?
        var side: Side
        var websiteUrl: String
        var packageType: PackageType
        // this entry got added as dependency for something else, only setthis if you know what you are doing
        var version: String // TODO: use regex only ?
        var transient: Boolean
        var fileName: String?
        var fileNameRegex: String
        var validMcVersions: Set<String>
        var invalidMcVersions: Set<String>

        fun id(): String?
        fun applyOverride(override: EntryOverride): Common

        fun toCommonComponent(defaultId: String? = null): CommonComponent = CommonComponent(
            id = id.takeUnless { it.isNullOrBlank() } ?: defaultId ?: error("id must be set on $this"),
            name = name,
            folder = folder,
            description = description,
            optionalData = optional?.toOptionalData(),
            side = side,
            websiteUrl = websiteUrl,
            dependencies = mutableMapOf(),
            packageType = packageType,
            transient = transient,
            version = version,
            fileName = fileName,
            fileNameRegex = fileNameRegex,
            validMcVersions = validMcVersions,
            invalidMcVersions = invalidMcVersions
        )

        fun applyCommonOverride(override: EntryOverride) {
            override.folder?.let { folder = it }
            override.description?.let { description = it }
            override.optional?.let { optionalOverride ->
                val opt = this.optional ?: Optional()
                opt.applyOverride(optionalOverride)
                optional = opt
            }
            override.side?.let { side = it }
            override.websiteUrl?.let { websiteUrl = it }
            override.packageType?.let { packageType = it }
            override.version?.let { version = it }
            override.fileName?.let { fileName = it }
            override.fileNameRegex?.let { fileNameRegex = it }
            override.validMcVersions?.let { validMcVersions += it }
            override.invalidMcVersions?.let { invalidMcVersions += it }
        }

        fun <E: Common> foldOverrides(overrides: Map<String, EntryOverride>): E {
            val intitalEntry = this as E
            val entryId = intitalEntry.id() ?: "null"
            val entry = intitalEntry.applyOverrides.fold(intitalEntry) { acc: E, overrideId ->
                val entryOverride =
                    overrides[overrideId] ?: error("$entryId: override for id $overrideId not found")
                return@fold acc.applyOverride(entryOverride) as E
            }
            return entry as E
        }
    }

    fun postParse(overrideKey: String): FileEntry = when (this) {
        is Curse -> {
            if (curse_projectName != null) {
                val addonid = Autocompletions.curseforge[curse_projectName]?.toIntOrNull()
                val newName = curse_projectName.substringAfterLast('/')
                require(addonid != null) { "cannot find replacement for $curse_projectName / ${Autocompletions.curseforge[curse_projectName]}" }
//                logger.trace { "before transform: $validMcVersions" }
                copy(
//                    curse_projectName = null,
                    applyOverrides = listOfNotNull(overrideKey.takeUnless { it.isBlank() }) + applyOverrides,
                    curse_projectID = ProjectID(addonid),
                    id = id.takeUnless { it.isNullOrBlank() } ?: newName,
                    name = name ?: newName
                )
            } else {
                this
            }.copy(
                applyOverrides = listOfNotNull(overrideKey.takeUnless { it.isBlank() }) + applyOverrides,
            )
        }
        is Direct -> copy(
            applyOverrides = listOfNotNull(overrideKey.takeUnless { it.isBlank() }) + applyOverrides,
        )
        is Jenkins -> copy(
            applyOverrides = listOfNotNull(overrideKey.takeUnless { it.isBlank() }) + applyOverrides,
        )
        is Local -> copy(
            applyOverrides = listOfNotNull(overrideKey.takeUnless { it.isBlank() }) + applyOverrides,
        )
        is Noop -> copy(
            applyOverrides = listOfNotNull(overrideKey.takeUnless { it.isBlank() }) + applyOverrides,
        )
    }

    abstract fun toEntry(overrides: Map<String, EntryOverride>): FlatEntry

    @Serializable
    @SerialName("curse")
    data class Curse(
        @JsonSchema.StringEnum(["replace_with_curseforge_projects"])
        val curse_projectName: String? = null,
        val curse_releaseTypes: Set<FileType> = setOf(
            FileType.Release,
            FileType.Beta
        ),
        val curse_projectID: ProjectID = ProjectID.INVALID,
        val curse_fileID: FileID = FileID.INVALID,
        val curse_useOriginalUrl: Boolean = true,
        val curse_skipFingerprintCheck: Boolean = false,

        @JsonSchema.Definition("entry.overridesList")
        @JsonSchema.StringEnum(["replace_with_overrides"])
        override val applyOverrides: List<String> = listOf(),

        override var id: String? = CommonComponent.DEFAULT.id,
        override var name: String? = CommonComponent.DEFAULT.name,
        override var folder: String? = CommonComponent.DEFAULT.folder,
        override var description: String? = CommonComponent.DEFAULT.description,
        override var optional: Optional? = null,
        override var side: Side = CommonComponent.DEFAULT.side,
        override var websiteUrl: String = CommonComponent.DEFAULT.websiteUrl,
        override var packageType: PackageType = CommonComponent.DEFAULT.packageType,
        override var transient: Boolean = CommonComponent.DEFAULT.transient,
        override var version: String = CommonComponent.DEFAULT.version,
        override var fileName: String? = CommonComponent.DEFAULT.fileName,
        override var fileNameRegex: String =CommonComponent.DEFAULT.fileNameRegex,
        override var validMcVersions: Set<String> = CommonComponent.DEFAULT.validMcVersions,
        override var invalidMcVersions: Set<String> = CommonComponent.DEFAULT.invalidMcVersions,
    ) : Common, FileEntry() {
        @JsonSchema.StringEnum(["curse"])
        private val type = "curse"
        override fun id() = id.takeUnless { it.isNullOrBlank() } ?: curse_projectName?.substringAfterLast('/')
        override fun applyOverride(override: EntryOverride): Curse {
            return when (override) {
                is EntryOverride.Curse -> copy(
                    curse_useOriginalUrl = override.curse_useOriginalUrl ?: curse_useOriginalUrl,
                    curse_skipFingerprintCheck = override.curse_skipFingerprintCheck ?: curse_skipFingerprintCheck,
                ).apply {
                    applyCommonOverride(override)
                }
                is EntryOverride.Common -> apply {
                    applyCommonOverride(override)
                }
                else -> this
            }
        }

        override fun toEntry(overrides: Map<String, EntryOverride>): FlatEntry =
            (foldOverrides(overrides) as Curse).let {
                FlatEntry.Curse(
                    common = it.toCommonComponent(it.curse_projectName?.substringAfterLast('/')),
                    curse = it.toCurseComponent()
                )
            }

        private fun toCurseComponent() = CurseComponent(
            releaseTypes = curse_releaseTypes,
            projectID = curse_projectID,
            fileID = curse_fileID,
            useOriginalUrl = curse_useOriginalUrl,
            skipFingerprintCheck = curse_skipFingerprintCheck
        )

//        override fun toString(): String {
//            return "FileEntry.Curse(projectName=$projectName,curse=$curse,validMcVersion=${common.validMcVersions})"
//        }
    }

    @Serializable
    @SerialName("direct")
    data class Direct(
        val direct_url: String, // = "",
        val direct_useOriginalUrl: Boolean = true,

        @JsonSchema.Definition("entry.overridesList")
        @JsonSchema.StringEnum(["replace_with_overrides"])
        override val applyOverrides: List<String> = listOf(),

        override var id: String? = CommonComponent.DEFAULT.id,
        override var name: String? = CommonComponent.DEFAULT.name,
        override var folder: String? = CommonComponent.DEFAULT.folder,
        override var description: String? = CommonComponent.DEFAULT.description,
        override var optional: Optional? = null,
        override var side: Side = CommonComponent.DEFAULT.side,
        override var websiteUrl: String = CommonComponent.DEFAULT.websiteUrl,
        override var packageType: PackageType = CommonComponent.DEFAULT.packageType,
        override var transient: Boolean = CommonComponent.DEFAULT.transient,
        override var version: String = CommonComponent.DEFAULT.version,
        override var fileName: String? = CommonComponent.DEFAULT.fileName,
        override var fileNameRegex: String =CommonComponent.DEFAULT.fileNameRegex,
        override var validMcVersions: Set<String> = CommonComponent.DEFAULT.validMcVersions,
        override var invalidMcVersions: Set<String> = CommonComponent.DEFAULT.invalidMcVersions,
    ) : Common, FileEntry() {
        @JsonSchema.StringEnum(["direct"])
        private val type = "direct"
        override fun id() = id.takeUnless { it.isNullOrBlank() }  ?: direct_url.split(":|&|=".toRegex()).joinToString("_")
        override fun applyOverride(override: EntryOverride): Direct {
            return when (override) {
                is EntryOverride.Direct -> copy(
//                    direct_url = override.url ?: direct_url,
                    direct_useOriginalUrl = override.direct_useOriginalUrl ?: direct_useOriginalUrl
                ).apply {
                    applyCommonOverride(override)
                }
                is EntryOverride.Common -> apply {
                    applyCommonOverride(override)
                }
                else -> this
            }
        }

        override fun toEntry(overrides: Map<String, EntryOverride>): FlatEntry =
            (foldOverrides(overrides) as Direct).let {
                FlatEntry.Direct(
                    common = it.toCommonComponent(it.id()),
                    direct = it.toDirectComponent()
                )
            }

        private fun toDirectComponent() = DirectComponent(
            url = direct_url,
            useOriginalUrl = direct_useOriginalUrl,
        )
    }

    @Serializable
    @SerialName("jenkins")
    data class Jenkins(
        val jenkins_jenkinsUrl: String = "",
        val jenkins_job: String, // = "",
        val jenkins_buildNumber: Int = -1,

        @JsonSchema.Definition("entry.overridesList")
        @JsonSchema.StringEnum(["replace_with_overrides"])
        override val applyOverrides: List<String> = listOf(),

        override var id: String? = CommonComponent.DEFAULT.id,
        override var name: String? = CommonComponent.DEFAULT.name,
        override var folder: String? = CommonComponent.DEFAULT.folder,
        override var description: String? = CommonComponent.DEFAULT.description,
        override var optional: Optional? = null,
        override var side: Side = CommonComponent.DEFAULT.side,
        override var websiteUrl: String = CommonComponent.DEFAULT.websiteUrl,
        override var packageType: PackageType = CommonComponent.DEFAULT.packageType,
        override var transient: Boolean = CommonComponent.DEFAULT.transient,
        override var version: String = CommonComponent.DEFAULT.version,
        override var fileName: String? = CommonComponent.DEFAULT.fileName,
        override var fileNameRegex: String =CommonComponent.DEFAULT.fileNameRegex,
        override var validMcVersions: Set<String> = CommonComponent.DEFAULT.validMcVersions,
        override var invalidMcVersions: Set<String> = CommonComponent.DEFAULT.invalidMcVersions,
    ) : Common, FileEntry() {
        @JsonSchema.StringEnum(["jenkins"])
        private val type = "jenkins"
        override fun id() = id.takeUnless { it.isNullOrBlank() }  ?: jenkins_job
        override fun applyOverride(override: EntryOverride): Jenkins {
            return when (override) {
                is EntryOverride.Jenkins -> copy(
                    jenkins_jenkinsUrl = override.jenkins_jenkinsUrl ?: jenkins_jenkinsUrl,
//                    jenkins_job = override.job ?: jenkins_job,
//                    jenkins_buildNumber = override.buildNumber ?: jenkins_buildNumber
                ).apply {
                    applyCommonOverride(override)
                }
                is EntryOverride.Common -> apply {
                    applyCommonOverride(override)
                }
                else -> this
            }
        }

        override fun toEntry(overrides: Map<String, EntryOverride>): FlatEntry =
            (foldOverrides(overrides) as Jenkins).let {
                FlatEntry.Jenkins(
                    common = it.toCommonComponent(it.id()),
                    jenkins = it.toJenkinsComponent()
                )
            }

        private fun toJenkinsComponent() = JenkinsComponent(
            jenkinsUrl = jenkins_jenkinsUrl,
            job = jenkins_job,
            buildNumber = jenkins_buildNumber
        )

    }

    @Serializable
    @SerialName("local")
    data class Local(
        val local_fileSrc: String, // = "",

        @JsonSchema.Definition("entry.overridesList")
        @JsonSchema.StringEnum(["replace_with_overrides"])
        override val applyOverrides: List<String> = listOf(),

        override var id: String? = CommonComponent.DEFAULT.id,
        override var name: String? = CommonComponent.DEFAULT.name,
        override var folder: String? = CommonComponent.DEFAULT.folder,
        override var description: String? = CommonComponent.DEFAULT.description,
        override var optional: Optional? = null,
        override var side: Side = CommonComponent.DEFAULT.side,
        override var websiteUrl: String = CommonComponent.DEFAULT.websiteUrl,
        override var packageType: PackageType = CommonComponent.DEFAULT.packageType,
        override var transient: Boolean = CommonComponent.DEFAULT.transient,
        override var version: String = CommonComponent.DEFAULT.version,
        override var fileName: String? = CommonComponent.DEFAULT.fileName,
        override var fileNameRegex: String =CommonComponent.DEFAULT.fileNameRegex,
        override var validMcVersions: Set<String> = CommonComponent.DEFAULT.validMcVersions,
        override var invalidMcVersions: Set<String> = CommonComponent.DEFAULT.invalidMcVersions,
    ) : Common, FileEntry() {
        @JsonSchema.StringEnum(["local"])
        private val type = "local"
        override fun id() = id.takeUnless { it.isNullOrBlank() } ?: local_fileSrc
        override fun applyOverride(override: EntryOverride): Local {
            return when (override) {
                is EntryOverride.Local -> copy(
                    local_fileSrc = override.local_fileSrc ?: local_fileSrc,
                ).apply {
                    applyCommonOverride(override)
                }
                is EntryOverride.Common -> apply {
                    applyCommonOverride(override)
                }
                else -> this
            }
        }

        override fun toEntry(overrides: Map<String, EntryOverride>): FlatEntry =
            (foldOverrides(overrides) as Local).let {
                FlatEntry.Local(
                    common = it.toCommonComponent(it.id()),
                    local = it.toLocalComponent()
                )
            }

        private fun toLocalComponent() = LocalComponent(
            fileSrc = local_fileSrc
        )

    }

    @Serializable
    @SerialName("noop")
    data class Noop(
        @JsonSchema.Definition("entry.overridesList")
        @JsonSchema.StringEnum(["replace_with_overrides"])
        override val applyOverrides: List<String> = listOf(),

        override var id: String? = CommonComponent.DEFAULT.id,
        override var name: String? = CommonComponent.DEFAULT.name,
        override var folder: String? = CommonComponent.DEFAULT.folder,
        override var description: String? = CommonComponent.DEFAULT.description,
        override var optional: Optional? = null,
        override var side: Side = CommonComponent.DEFAULT.side,
        override var websiteUrl: String = CommonComponent.DEFAULT.websiteUrl,
        override var packageType: PackageType = CommonComponent.DEFAULT.packageType,
        override var transient: Boolean = CommonComponent.DEFAULT.transient,
        override var version: String = CommonComponent.DEFAULT.version,
        override var fileName: String? = CommonComponent.DEFAULT.fileName,
        override var fileNameRegex: String =CommonComponent.DEFAULT.fileNameRegex,
        override var validMcVersions: Set<String> = CommonComponent.DEFAULT.validMcVersions,
        override var invalidMcVersions: Set<String> = CommonComponent.DEFAULT.invalidMcVersions,
    ) : Common, FileEntry() {
        @JsonSchema.StringEnum(["noop"])
        private val type = "noop"
        override fun id() = id
        override fun applyOverride(override: EntryOverride): Noop {
            return when (override) {
                is EntryOverride.Common -> apply {
                    applyCommonOverride(override)
                }
                else -> this
            }
        }

        override fun toEntry(overrides: Map<String, EntryOverride>): FlatEntry =
            (foldOverrides(overrides) as Noop).let {
                FlatEntry.Noop(
                    common = it.toCommonComponent(it.id()),
                )
            }

    }

//    @Serializable
//    @SerialName("nested")
//    data class Nested(
//        @SerialName("commonOverrides")
//        val common: EntryOverride.Common? = null,
//        @SerialName("curseOverrides")
//        val curse: EntryOverride.Curse? = null,
//        @SerialName("directOverrides")
//        val direct: EntryOverride.Direct? = null,
//        @SerialName("jenkinsOverrides")
//        val jenkins: EntryOverride.Jenkins? = null,
//        @SerialName("localOverrides")
//        val local: EntryOverride.Local? = null,
//        @JsonSchema.Definition("FileEntryList")
//        val mods: List<JsonElement>
//    ) : FileEntry() {
//        override fun getId(): String {
//            return "nested_$this"
//        }
//        override fun applyOverride(override: EntryOverride): Nested {
//            // combine with existing overrides
//
//            return when (override) {
//                is EntryOverride.Curse -> {
//                    if(curse == null)
//                        copy(curse = override)
//                    else
//                        copy(curse = curse + override)
//                }
//                is EntryOverride.Jenkins -> {
//                    if(jenkins == null)
//                        copy(jenkins = override)
//                    else
//                        copy(jenkins = jenkins + override)
//                }
//                is EntryOverride.Direct -> {
//                    if(direct == null)
//                        copy(direct = override)
//                    else
//                        copy(direct = direct + override)
//                }
//                is EntryOverride.Local -> {
//                    if(local == null)
//                        copy(local = override)
//                    else
//                        copy(local = local + override)
//                }
//                is EntryOverride.Common -> {
//                    if(common == null)
//                        copy(common = override)
//                    else
//                        copy(
//                            common = common + override
//                        )
//                }
//                else -> error("unhanndled override type: $override")
//            }
//        }
//
//        //TODO: return multiple entries
//        override fun toEntry(overrides: Map<String, EntryOverride>): List<FlatEntry> = mods.flatMap { jsonElement ->
//            //TODO: apply common settings to subEntry
//
//            var subEntry = VersionPack.parseEntry(jsonElement).postParse()
//
//            curse?.also { subEntry = subEntry.applyOverride(it) }
//            jenkins?.also { subEntry = subEntry.applyOverride(it) }
//            direct?.also { subEntry = subEntry.applyOverride(it) }
//            local?.also { subEntry = subEntry.applyOverride(it) }
//            common?.also { subEntry = subEntry.applyOverride(it) }
//
//            subEntry.toEntry(overrides)
//        }
//    }
}