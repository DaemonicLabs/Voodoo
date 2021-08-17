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
import voodoo.labrinth.ModId
import voodoo.labrinth.VersionId

@Serializable
sealed class FileEntry {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    data class CommonBlob(
        val applyOverrides: List<String>,
        val id: String?,
        val name: String?,
        val folder: String?,
        val description: String?,
        val optional: Optional?,
        val side: Side,
        val websiteUrl: String?,
        val packageType: PackageType,
        val version: String?,
        val transient: Boolean,
        val fileName: String?,
        val fileNameRegex: String,
        val validMcVersions: Set<String>,
        val invalidMcVersions: Set<String>,
    )

    abstract val applyOverrides: List<String>
    abstract val id: String?
    abstract val name: String?
    abstract val folder: String?
    abstract val description: String?
    abstract val optional: Optional?
    abstract val side: Side
    abstract val websiteUrl: String?
    abstract val packageType: PackageType
    abstract val version: String?
    abstract val transient: Boolean
    abstract val fileName: String?
    abstract val fileNameRegex: String
    abstract val validMcVersions: Set<String>
    abstract val invalidMcVersions: Set<String>

    val common: CommonBlob get() = CommonBlob(
        applyOverrides = applyOverrides,
        id  = id,
        name  = name,
        folder  = folder,
        description  = description,
        optional  = optional,
        side =  side,
        websiteUrl =  websiteUrl,
        packageType =  packageType,
        transient =  transient,
        version =  version,
        fileName  = fileName,
        fileNameRegex =  fileNameRegex,
        validMcVersions =  validMcVersions,
        invalidMcVersions =  invalidMcVersions,
    )

    abstract fun id(): String?
    abstract fun typeKey(): String
    abstract fun applyOverride(override: EntryOverride): FileEntry

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

    abstract fun <E : FileEntry> assignCommonValues(commonBlob: CommonBlob): E

    protected fun <E : FileEntry> applyCommonOverride(override: EntryOverride): E {
        var c = common

        override.folder?.let { c = c.copy(folder = it) }
        override.description?.let { c = c.copy(description = it) }
        override.optional?.let { optionalOverride ->
            val opt = this.optional ?: Optional()
            opt.applyOverride(optionalOverride)
            c = c.copy(optional = opt)
        }
        override.side?.let { c = c.copy(side = it) }
        override.websiteUrl?.let { c = c.copy(websiteUrl = it) }
        override.packageType?.let { c = c.copy(packageType = it) }
        override.version?.let { c = c.copy(version = it) }
        override.fileName?.let { c = c.copy(fileName = it) }
        override.fileNameRegex?.let { c = c.copy(fileNameRegex = it) }
        override.validMcVersions?.let { c = c.copy(validMcVersions = c.validMcVersions + it) }
        override.invalidMcVersions?.let { c = c.copy(invalidMcVersions = c.invalidMcVersions + it) }
        return assignCommonValues(c)
    }

    fun <E : FileEntry> foldOverrides(overrides: Map<String, EntryOverride>): E {
        val inititalEntry = this as E
        val entryId = inititalEntry.id().takeUnless { it.isNullOrBlank() } ?: error("missing entry id for entry: ")
        val defaultEntryOverrides = listOf(
            "", "@common", "@" + inititalEntry.typeKey()
        ).mapNotNull { key ->
            overrides[key]
        }
        val entryWithDefaultOverrides = defaultEntryOverrides.fold(inititalEntry) { e, entryOverride ->
            e.applyOverride(entryOverride) as E
        }

        val entry = inititalEntry.applyOverrides.fold(entryWithDefaultOverrides) { acc: E, overrideId ->
            val overrideKeys = listOf(
                overrideId,
                overrideId + "@common",
                overrideId + "@" + acc.typeKey()
            )
            val entryOverrides = overrideKeys.mapNotNull { key ->
                overrides[key]
            }
            if(entryOverrides.isEmpty()) {
                error("$entryId: override for ids $overrideKeys not found")
            }
            return@fold entryOverrides.fold(acc) { e, entryOverride ->
                e.applyOverride(entryOverride) as E
            }
        }
        return entry as E
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
//                    id = id.takeUnless { it.isNullOrBlank() } ?: newName,
                    name = name ?: newName
                )
            } else {
                this
            }.copy(
                applyOverrides = listOfNotNull(overrideKey.takeUnless { it.isBlank() }) + applyOverrides,
            )
        }
        is Modrinth -> {
//            if (modrinth_slug != null) {
//                //TODO: load autocompletions here
//                val addonid = Autocompletions.curseforge[curse_projectName]?.toIntOrNull()
//                val newName = curse_projectName.substringAfterLast('/')
//                require(addonid != null) { "cannot find replacement for $modrinth_slug / ${Autocompletions.curseforge[curse_projectName]}" }
//                copy(
//                    applyOverrides = listOfNotNull(overrideKey.takeUnless { it.isBlank() }) + applyOverrides,
//                    modrinth_modId = ModId(addonid),
//                    name = name ?: newName
//                )
//            } else {
//                this
//            }.copy(
//                applyOverrides = listOfNotNull(overrideKey.takeUnless { it.isBlank() }) + applyOverrides,
//            )
            copy(
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

        override val id: String? = null,
        override val name: String? = CommonComponent.DEFAULT.name,
        override val folder: String? = CommonComponent.DEFAULT.folder,
        override val description: String? = CommonComponent.DEFAULT.description,
        override val optional: Optional? = null,
        override val side: Side = CommonComponent.DEFAULT.side,
        override val websiteUrl: String? = CommonComponent.DEFAULT.websiteUrl,
        override val packageType: PackageType = CommonComponent.DEFAULT.packageType,
        override val transient: Boolean = CommonComponent.DEFAULT.transient,
        override val version: String? = CommonComponent.DEFAULT.version,
        override val fileName: String? = CommonComponent.DEFAULT.fileName,
        override val fileNameRegex: String = CommonComponent.DEFAULT.fileNameRegex,
        override val validMcVersions: Set<String> = CommonComponent.DEFAULT.validMcVersions,
        override val invalidMcVersions: Set<String> = CommonComponent.DEFAULT.invalidMcVersions,
    ) : FileEntry() {
        override fun id() = id.takeUnless { it.isNullOrBlank() } ?: curse_projectName?.substringAfterLast('/')
        override fun typeKey(): String = "curse"
        override fun applyOverride(override: EntryOverride): Curse {
            return when (override) {
                is EntryOverride.Curse -> copy(
                    curse_useOriginalUrl = override.curse_useOriginalUrl ?: curse_useOriginalUrl,
                    curse_skipFingerprintCheck = override.curse_skipFingerprintCheck ?: curse_skipFingerprintCheck,
                ).run {
                    applyCommonOverride(override)
                }
                is EntryOverride.Common -> run {
                    applyCommonOverride(override) as Curse
                }
                else -> this
            }
        }

        override fun <E : FileEntry> assignCommonValues(commonBlob: CommonBlob): E {
            return copy(
                id = commonBlob.id,
                name = commonBlob.name,
                folder = commonBlob.folder,
                description = commonBlob.description,
                optional = commonBlob.optional,
                side = commonBlob.side,
                websiteUrl = commonBlob.websiteUrl,
                packageType = commonBlob.packageType,
                transient = commonBlob.transient,
                version = commonBlob.version,
                fileName = commonBlob.fileName,
                fileNameRegex = commonBlob.fileNameRegex,
                validMcVersions = commonBlob.validMcVersions,
                invalidMcVersions = commonBlob.invalidMcVersions,
            ) as E
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
    @SerialName("modrinth")
    data class Modrinth(
        @JsonSchema.StringEnum(["replace_with_modrinth_projects"])
        val modrinth_slug: String? = null,
        val modrinth_releaseTypes: Set<FileType> = setOf(
            FileType.Release,
            FileType.Beta
        ),
        val modrinth_modId: ModId = ModId.INVALID,
        val modrinth_versionId: VersionId = VersionId.INVALID,
        val modrinth_useOriginalUrl: Boolean = true,
        val modrinth_skipFingerprintCheck: Boolean = false,

        @JsonSchema.Definition("entry.overridesList")
        @JsonSchema.StringEnum(["replace_with_overrides"])
        override val applyOverrides: List<String> = listOf(),

        override val id: String? = null,
        override val name: String? = CommonComponent.DEFAULT.name,
        override val folder: String? = CommonComponent.DEFAULT.folder,
        override val description: String? = CommonComponent.DEFAULT.description,
        override val optional: Optional? = null,
        override val side: Side = CommonComponent.DEFAULT.side,
        override val websiteUrl: String? = CommonComponent.DEFAULT.websiteUrl,
        override val packageType: PackageType = CommonComponent.DEFAULT.packageType,
        override val transient: Boolean = CommonComponent.DEFAULT.transient,
        override val version: String? = CommonComponent.DEFAULT.version,
        override val fileName: String? = CommonComponent.DEFAULT.fileName,
        override val fileNameRegex: String = CommonComponent.DEFAULT.fileNameRegex,
        override val validMcVersions: Set<String> = CommonComponent.DEFAULT.validMcVersions,
        override val invalidMcVersions: Set<String> = CommonComponent.DEFAULT.invalidMcVersions,
    ) : FileEntry() {
        override fun id() = id.takeUnless { it.isNullOrBlank() } ?: modrinth_slug
        override fun typeKey(): String = "modrinth"
        override fun applyOverride(override: EntryOverride): Modrinth {
            return when (override) {
                is EntryOverride.Modrinth -> copy(
                    modrinth_useOriginalUrl = override.modrinth_useOriginalUrl ?: modrinth_useOriginalUrl,
                    modrinth_skipFingerprintCheck = override.modrinth_skipFingerprintCheck ?: modrinth_skipFingerprintCheck,
                ).run {
                    applyCommonOverride(override)
                }
                is EntryOverride.Common -> run {
                    applyCommonOverride(override) as Modrinth
                }
                else -> this
            }
        }

        override fun <E : FileEntry> assignCommonValues(commonBlob: CommonBlob): E {
            return copy(
                id = commonBlob.id,
                name = commonBlob.name,
                folder = commonBlob.folder,
                description = commonBlob.description,
                optional = commonBlob.optional,
                side = commonBlob.side,
                websiteUrl = commonBlob.websiteUrl,
                packageType = commonBlob.packageType,
                transient = commonBlob.transient,
                version = commonBlob.version,
                fileName = commonBlob.fileName,
                fileNameRegex = commonBlob.fileNameRegex,
                validMcVersions = commonBlob.validMcVersions,
                invalidMcVersions = commonBlob.invalidMcVersions,
            ) as E
        }

        override fun toEntry(overrides: Map<String, EntryOverride>): FlatEntry =
            (foldOverrides(overrides) as Modrinth).let {
                FlatEntry.Modrinth(
                    common = it.toCommonComponent(it.modrinth_slug),
                    modrinth = it.toModrinthComponent()
                )
            }

        private fun toModrinthComponent() = ModrinthComponent(
            releaseTypes = modrinth_releaseTypes,
            slug = modrinth_slug ?: "", //TODO: either make nullable or find better default
            modId = modrinth_modId,
            versionId = modrinth_versionId,
            useOriginalUrl = modrinth_useOriginalUrl,
            skipFingerprintCheck = modrinth_skipFingerprintCheck
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

        override val id: String? = null,
        override val name: String? = CommonComponent.DEFAULT.name,
        override val folder: String? = CommonComponent.DEFAULT.folder,
        override val description: String? = CommonComponent.DEFAULT.description,
        override val optional: Optional? = null,
        override val side: Side = CommonComponent.DEFAULT.side,
        override val websiteUrl: String? = CommonComponent.DEFAULT.websiteUrl,
        override val packageType: PackageType = CommonComponent.DEFAULT.packageType,
        override val transient: Boolean = CommonComponent.DEFAULT.transient,
        override val version: String? = CommonComponent.DEFAULT.version,
        override val fileName: String? = CommonComponent.DEFAULT.fileName,
        override val fileNameRegex: String = CommonComponent.DEFAULT.fileNameRegex,
        override val validMcVersions: Set<String> = CommonComponent.DEFAULT.validMcVersions,
        override val invalidMcVersions: Set<String> = CommonComponent.DEFAULT.invalidMcVersions,
    ) : FileEntry() {
        override fun id() =
            id.takeUnless { it.isNullOrBlank() } ?: direct_url.split(":|&|=".toRegex()).joinToString("_")
        override fun typeKey(): String = "direct"

        override fun applyOverride(override: EntryOverride): Direct {
            return when (override) {
                is EntryOverride.Direct -> copy(
//                    direct_url = override.url ?: direct_url,
                    direct_useOriginalUrl = override.direct_useOriginalUrl ?: direct_useOriginalUrl
                ).run {
                    applyCommonOverride(override)
                }
                is EntryOverride.Common -> run {
                    applyCommonOverride(override)
                }
                else -> this
            }
        }

        override fun <E : FileEntry> assignCommonValues(commonBlob: CommonBlob): E {
            return copy(
                id = commonBlob.id,
                name = commonBlob.name,
                folder = commonBlob.folder,
                description = commonBlob.description,
                optional = commonBlob.optional,
                side = commonBlob.side,
                websiteUrl = commonBlob.websiteUrl,
                packageType = commonBlob.packageType,
                transient = commonBlob.transient,
                version = commonBlob.version,
                fileName = commonBlob.fileName,
                fileNameRegex = commonBlob.fileNameRegex,
                validMcVersions = commonBlob.validMcVersions,
                invalidMcVersions = commonBlob.invalidMcVersions,
            ) as E
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
        val jenkins_jenkinsUrl: String? = null,
        val jenkins_job: String,
        val jenkins_buildNumber: Int? = null,
        val jenkins_useOriginalUrl: Boolean = true,

        @JsonSchema.Definition("entry.overridesList")
        @JsonSchema.StringEnum(["replace_with_overrides"])
        override val applyOverrides: List<String> = listOf(),

        override val id: String? = null,
        override val name: String? = CommonComponent.DEFAULT.name,
        override val folder: String? = CommonComponent.DEFAULT.folder,
        override val description: String? = CommonComponent.DEFAULT.description,
        override val optional: Optional? = null,
        override val side: Side = CommonComponent.DEFAULT.side,
        override val websiteUrl: String? = CommonComponent.DEFAULT.websiteUrl,
        override val packageType: PackageType = CommonComponent.DEFAULT.packageType,
        override val transient: Boolean = CommonComponent.DEFAULT.transient,
        override val version: String? = CommonComponent.DEFAULT.version,
        override val fileName: String? = CommonComponent.DEFAULT.fileName,
        override val fileNameRegex: String = CommonComponent.DEFAULT.fileNameRegex,
        override val validMcVersions: Set<String> = CommonComponent.DEFAULT.validMcVersions,
        override val invalidMcVersions: Set<String> = CommonComponent.DEFAULT.invalidMcVersions,
    ) : FileEntry() {
        override fun id() = id.takeUnless { it.isNullOrBlank() } ?: jenkins_job
        override fun typeKey(): String = "jenkins"
        override fun applyOverride(override: EntryOverride): Jenkins {
            return when (override) {
                is EntryOverride.Jenkins -> copy(
                    jenkins_jenkinsUrl = override.jenkins_jenkinsUrl ?: jenkins_jenkinsUrl,
//                    jenkins_job = override.job ?: jenkins_job,
//                    jenkins_buildNumber = override.buildNumber ?: jenkins_buildNumber
                    jenkins_useOriginalUrl = override.jenkins_useOriginalUrl ?: jenkins_useOriginalUrl
                ).run {
                    applyCommonOverride(override)
                }
                is EntryOverride.Common -> run {
                    applyCommonOverride(override)
                }
                else -> this
            }
        }

        override fun <E : FileEntry> assignCommonValues(commonBlob: CommonBlob): E {
            return copy(
                id = commonBlob.id,
                name = commonBlob.name,
                folder = commonBlob.folder,
                description = commonBlob.description,
                optional = commonBlob.optional,
                side = commonBlob.side,
                websiteUrl = commonBlob.websiteUrl,
                packageType = commonBlob.packageType,
                transient = commonBlob.transient,
                version = commonBlob.version,
                fileName = commonBlob.fileName,
                fileNameRegex = commonBlob.fileNameRegex,
                validMcVersions = commonBlob.validMcVersions,
                invalidMcVersions = commonBlob.invalidMcVersions,
            ) as E
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
            buildNumber = jenkins_buildNumber,
            useOriginalUrl = jenkins_useOriginalUrl,
        )

    }

    @Serializable
    @SerialName("local")
    data class Local(
        val local_fileSrc: String, // = "",

        @JsonSchema.Definition("entry.overridesList")
        @JsonSchema.StringEnum(["replace_with_overrides"])
        override val applyOverrides: List<String> = listOf(),

        override val id: String? = null,
        override val name: String? = CommonComponent.DEFAULT.name,
        override val folder: String? = CommonComponent.DEFAULT.folder,
        override val description: String? = CommonComponent.DEFAULT.description,
        override val optional: Optional? = null,
        override val side: Side = CommonComponent.DEFAULT.side,
        override val websiteUrl: String? = CommonComponent.DEFAULT.websiteUrl,
        override val packageType: PackageType = CommonComponent.DEFAULT.packageType,
        override val transient: Boolean = CommonComponent.DEFAULT.transient,
        override val version: String? = CommonComponent.DEFAULT.version,
        override val fileName: String? = CommonComponent.DEFAULT.fileName,
        override val fileNameRegex: String = CommonComponent.DEFAULT.fileNameRegex,
        override val validMcVersions: Set<String> = CommonComponent.DEFAULT.validMcVersions,
        override val invalidMcVersions: Set<String> = CommonComponent.DEFAULT.invalidMcVersions,
    ) : FileEntry() {
        override fun id() = id.takeUnless { it.isNullOrBlank() } ?: local_fileSrc
        override fun typeKey(): String = "local"
        override fun applyOverride(override: EntryOverride): Local {
            return when (override) {
                is EntryOverride.Local -> copy(
                    local_fileSrc = override.local_fileSrc ?: local_fileSrc,
                ).run {
                    applyCommonOverride(override)
                }
                is EntryOverride.Common -> run {
                    applyCommonOverride(override)
                }
                else -> this
            }
        }

        override fun <E : FileEntry> assignCommonValues(commonBlob: CommonBlob): E {
            return copy(
                id = commonBlob.id,
                name = commonBlob.name,
                folder = commonBlob.folder,
                description = commonBlob.description,
                optional = commonBlob.optional,
                side = commonBlob.side,
                websiteUrl = commonBlob.websiteUrl,
                packageType = commonBlob.packageType,
                transient = commonBlob.transient,
                version = commonBlob.version,
                fileName = commonBlob.fileName,
                fileNameRegex = commonBlob.fileNameRegex,
                validMcVersions = commonBlob.validMcVersions,
                invalidMcVersions = commonBlob.invalidMcVersions,
            ) as E
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

        override val id: String? = null,
        override val name: String? = CommonComponent.DEFAULT.name,
        override val folder: String? = CommonComponent.DEFAULT.folder,
        override val description: String? = CommonComponent.DEFAULT.description,
        override val optional: Optional? = null,
        override val side: Side = CommonComponent.DEFAULT.side,
        override val websiteUrl: String? = CommonComponent.DEFAULT.websiteUrl,
        override val packageType: PackageType = CommonComponent.DEFAULT.packageType,
        override val transient: Boolean = CommonComponent.DEFAULT.transient,
        override val version: String? = CommonComponent.DEFAULT.version,
        override val fileName: String? = CommonComponent.DEFAULT.fileName,
        override val fileNameRegex: String = CommonComponent.DEFAULT.fileNameRegex,
        override val validMcVersions: Set<String> = CommonComponent.DEFAULT.validMcVersions,
        override val invalidMcVersions: Set<String> = CommonComponent.DEFAULT.invalidMcVersions,
    ) : FileEntry() {
        override fun id() = id
        override fun typeKey(): String = "none"
        override fun applyOverride(override: EntryOverride): Noop {
            return when (override) {
                is EntryOverride.Common -> run {
                    applyCommonOverride(override)
                }
                else -> this
            }
        }

        override fun <E : FileEntry> assignCommonValues(commonBlob: CommonBlob): E {
            return copy(
                id = commonBlob.id,
                name = commonBlob.name,
                folder = commonBlob.folder,
                description = commonBlob.description,
                optional = commonBlob.optional,
                side = commonBlob.side,
                websiteUrl = commonBlob.websiteUrl,
                packageType = commonBlob.packageType,
                transient = commonBlob.transient,
                version = commonBlob.version,
                fileName = commonBlob.fileName,
                fileNameRegex = commonBlob.fileNameRegex,
                validMcVersions = commonBlob.validMcVersions,
                invalidMcVersions = commonBlob.invalidMcVersions,
            ) as E
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