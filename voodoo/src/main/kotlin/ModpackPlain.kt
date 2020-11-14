import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import voodoo.data.DependencyType
import voodoo.data.Side
import voodoo.data.curse.FileID
import voodoo.data.curse.FileType
import voodoo.data.curse.PackageType
import voodoo.data.curse.ProjectID
import voodoo.poet.Poet
import voodoo.poet.generator.CurseSection

@Serializable
data class ModpackPlain(
    val title: String,
    val authors: List<String> = listOf(),
    val version: String,
    val icon: String,
    val mcVersion: String,
    val generators: Map<String, Generator> = mapOf(),
    val modloader: Modloader,
    @JsonSchema.Description(["url pointing to \$modpackId.package.json", "you need to upload the packaged modpack there"])
    val selfupdateUrl: String,
    val tags: Map<String, PlainTag>,
    val mods: Map<String, PlainEntry>
) {
    @Required
    var `$schema` = "./modpack.schema.json"
}

@Serializable
sealed class Generator() {
    abstract val mcVersions: List<String>
    @Serializable
    @SerialName("generator.curse")
    data class Curse(
        val section: CurseSection,
        val categories: List<String> = emptyList(),
        override val mcVersions: List<String> = emptyList()
    ) : Generator()

    /**
     * generates Fabric versions
     */
    @Serializable
    @SerialName("generator.fabric")
    data class Fabric(
        val stable: Boolean = true,
        override val mcVersions: List<String> = emptyList()
    ): Generator()

    @Serializable
    @SerialName("generator.forge")
    data class Forge(
        override val mcVersions: List<String> = emptyList()
    ): Generator()
}


@Serializable
sealed class PlainTag {
    @Serializable
    @SerialName("common")
    open class Common(
        val folder: String? = null,
        val description: String? = null,
        val optional: OptionalTag? = null,
        val side: Side? = null,
        val websiteUrl: String? = null,
        val packageType: PackageType? = null,
        val version: String? = null,
        val fileName: String? = null,
        val fileNameRegex: String? = null,
        val validMcVersions: Set<String>? = null,
        val invalidMcVersions: Set<String>? = null,
    ) : PlainTag() {

    }

    @Serializable
    @SerialName("curse")
    data class Curse(
        val releaseTypes: Set<FileType>? = null,
        val projectID: ProjectID? = null,
        val fileID: FileID? = null,
        val useOriginalUrl: Boolean? = null,
        val skipFingerprintCheck: Boolean? = null,
    ) : Common() {
    }

    @Serializable
    @SerialName("direct")
    data class Direct(
        val url: String? = null,
        val useOriginalUrl: Boolean? = null,
    ) : Common() {
    }

    @Serializable
    @SerialName("jenkins")
    data class Jenkins(
        val jenkinsUrl: String? = null,
        val job: String? = null,
        val buildNumber: Int? = null
    ) : Common() {

    }

    @Serializable
    @SerialName("local")
    data class Local(
        val fileSrc: String? = null
    ) : Common() {

    }
}

@Serializable
sealed class PlainEntry(
    @JsonSchema.DefinitionRef("entry.apply")
    @JsonSchema.StringEnum(["replace_with_tags"])
    val apply: List<String> = listOf(),
    var name: String? = null,
    var folder: String? = null,
    var description: String? = null,
    var optional: Optional? = null,
    var side: Side = Side.BOTH,
    var websiteUrl: String = "",
    var dependencies: MutableMap<String, DependencyType> = mutableMapOf(),
    var packageType: PackageType = PackageType.MOD,
    var transient: Boolean = false, // this entry got added as dependency for something else, only setthis if you know what you are doing
    var version: String = "", // TODO: use regex only ?
    var fileName: String? = null,
    var fileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$",
    var validMcVersions: Set<String> = setOf(),
    var invalidMcVersions: Set<String> = setOf(),
    val enabled: Boolean = true
) {
    protected fun applyCommonTag(tag: PlainTag.Common) {
        tag.folder?.let {
            folder = it
        }
        tag.description?.let {
            description = it
        }
        tag.optional?.let { optTag ->
            val opt = this.optional ?: Optional()
            optTag.selected?.let {
                opt.selected = it
            }
            optTag.recommendation?.let {
                opt.recommendation = it
            }
            optTag.files.let { files ->
                opt.files.include += files.include
                opt.files.exclude += files.exclude
                opt.files.flags += files.flags
            }
        }
        tag.side?.let {
            side = it
        }
        tag.websiteUrl?.let {
            websiteUrl = it
        }
        tag.packageType?.let {
            packageType = it
        }
        tag.version?.let {
            version = it
        }
        tag.fileName?.let {
            fileName = it
        }
        tag.fileNameRegex?.let {
            fileNameRegex = it
        }
        tag.validMcVersions?.let {
            validMcVersions += it
        }
        tag.invalidMcVersions?.let {
            invalidMcVersions += it
        }
    }

    abstract fun applyTag(tag: PlainTag.Common): PlainEntry

    @Serializable
    @SerialName("curse")
    data class Curse(
        val releaseTypes: Set<FileType> = setOf(FileType.Beta, FileType.Release),
        @JsonSchema.StringEnum(["replace_with_projectnames"])
        val projectName: String? = null,
        val projectID: ProjectID? = null,
        val fileID: FileID? = null,
        val useOriginalUrl: Boolean = true,
        val skipFingerprintCheck: Boolean = false,
    ) : PlainEntry() {
        fun applyTag(tag: PlainTag.Curse): Curse {
            return copy(
                releaseTypes = tag.releaseTypes ?: releaseTypes,
                projectID = tag.projectID ?: projectID,
                fileID = tag.fileID ?: fileID,
                useOriginalUrl = tag.useOriginalUrl ?: useOriginalUrl,
                skipFingerprintCheck = tag.skipFingerprintCheck ?: skipFingerprintCheck,
            ).apply {
                applyCommonTag(tag)
            }
        }

        override fun applyTag(tag: PlainTag.Common): Curse {
            return apply {
                applyCommonTag(tag)
            }
        }
    }

    @Serializable
    @SerialName("direct")
    data class Direct(
        val url: String? = null,
        val useOriginalUrl: Boolean = true
    ) : PlainEntry() {
        fun applyTag(tag: PlainTag.Direct): Direct {
            return copy(
                url = tag.url ?: url,
                useOriginalUrl = tag.useOriginalUrl ?: useOriginalUrl
            ).apply {
                applyCommonTag(tag)
            }
        }
        override fun applyTag(tag: PlainTag.Common): Direct {
            return apply {
                applyCommonTag(tag)
            }
        }
    }

    @Serializable
    @SerialName("jenkins")
    data class Jenkins(
        val jenkinsUrl: String = "",
        val job: String = "",
        val buildNumber: Int = -1
    ) : PlainEntry() {
        fun applyTag(tag: PlainTag.Jenkins): Jenkins {
            return copy(
                jenkinsUrl = tag.jenkinsUrl ?: jenkinsUrl,
                job = tag.job ?: job,
                buildNumber = tag.buildNumber ?: buildNumber
            ).apply {
                applyCommonTag(tag)
            }
        }
        override fun applyTag(tag: PlainTag.Common): Jenkins {
            return apply {
                applyCommonTag(tag)
            }
        }
    }

    @Serializable
    @SerialName("local")
    data class Local(
        val fileSrc: String = ""
    ) : PlainEntry() {
        fun applyTag(tag: PlainTag.Local): Local {
            return copy(
                fileSrc = tag.fileSrc ?: fileSrc
            ).apply {
                applyCommonTag(tag)
            }
        }
        override fun applyTag(tag: PlainTag.Common): Local {
            return apply {
                applyCommonTag(tag)
            }
        }
    }

    @Serializable
    @SerialName("noop")
    class Noop() : PlainEntry() {
        override fun applyTag(tag: PlainTag.Common): Noop {
            return apply {
                applyCommonTag(tag)
            }
        }
    }
}