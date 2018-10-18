package voodoo.data.flat

import com.skcraft.launcher.model.modpack.Feature
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerialSaver
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import kotlinx.serialization.UpdateMode
import kotlinx.serialization.internal.EnumSerializer
import kotlinx.serialization.internal.HashMapSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import kotlinx.serialization.set
import mu.KLogging
import voodoo.data.Side
import voodoo.data.curse.CurseConstants.PROXY_URL
import voodoo.data.curse.DependencyType
import voodoo.data.curse.FileID
import voodoo.data.curse.FileType
import voodoo.data.curse.PackageType
import voodoo.data.curse.ProjectID
import voodoo.data.lock.LockEntry
import voodoo.data.provider.UpdateChannel
import voodoo.util.equalsIgnoreCase
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

// @JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Serializable
data class Entry(
    // @JsonInclude(JsonInclude.Include.ALWAYS)
    val provider: String,
    var id: String,
    @Optional var name: String = "", // TODO add `by provider.getDisplayname(this)`
    @Optional var folder: String = "mods",
    @Optional var comment: String = "",
    @Optional var description: String = "",
    @Optional @Serializable(with = Feature.Companion::class) var feature: Feature? = null,
    @Optional var side: Side = Side.BOTH,
    @Optional var websiteUrl: String = "",
    @Optional var dependencies: MutableMap<DependencyType, List<String>> = mutableMapOf(),
    @Optional var replaceDependencies: Map<String, String> = mapOf(),
    // @JsonInclude(JsonInclude.Include.ALWAYS)
//        @Optional var optional: Boolean = feature != null,
    @Optional var packageType: PackageType = PackageType.MOD,
    @Optional var transient: Boolean = false, // this entry got added as dependency for something else
    @Optional var version: String = "", // TODO: use regex only ?
    @Optional var fileName: String? = null,
    @Optional var fileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$",
//        when {
//            provider.equals("CURSE", true) -> ".*(?<!-deobf\\.jar)\$"
//            provider.equals("JENKINS", true) -> ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$"
//            else -> ".*"
//        },
    @Optional var validMcVersions: Set<String> = setOf(),
    // CURSE
    @Optional var curseMetaUrl: String = PROXY_URL,
    @Optional var curseReleaseTypes: Set<FileType> = setOf(FileType.RELEASE, FileType.BETA),
    @Optional var curseProjectID: ProjectID = ProjectID.INVALID,
    @Optional var curseFileID: FileID = FileID.INVALID,
    // DIRECT
    @Optional var url: String = "",
    @Optional var useUrlTxt: Boolean = true,
    // JENKINS
    @Optional var jenkinsUrl: String = "",
    @Optional var job: String = "",
    @Optional var buildNumber: Int = -1,
    // LOCAL
    @Optional var fileSrc: String = "",
    // UPDATE-JSON
    @Optional var updateJson: String = "",
    @Optional var updateChannel: UpdateChannel = UpdateChannel.RECOMMENDED,
    @Optional var template: String = ""
) {

    @Serializer(forClass = Entry::class)
    companion object : KLogging() {
        override fun save(output: KOutput, obj: Entry) {
            val elemOutput = output.writeBegin(serialClassDesc)
            elemOutput.writeStringElementValue(serialClassDesc, 0, obj.provider)
            elemOutput.writeStringElementValue(serialClassDesc, 1, obj.id)
            with(Entry(provider = obj.provider, id = obj.id)) {
                elemOutput.serialize(this.name, obj.name, 2)
                elemOutput.serialize(this.folder, obj.folder, 3)
                elemOutput.serialize(this.comment, obj.comment, 4)
                elemOutput.serialize(this.description, obj.description, 5)
                if (this.feature != obj.feature) {
                    elemOutput.writeElement(serialClassDesc, 6)
                    elemOutput.write(Feature::class.serializer(), obj.feature!!)
                }
                elemOutput.serializeObj(this.side, obj.side, EnumSerializer(Side::class), 7)
                elemOutput.serialize(this.websiteUrl, obj.websiteUrl, 8)
                elemOutput.serializeObj(
                    this.dependencies.toMap(), obj.dependencies.toMap(), HashMapSerializer(
                        EnumSerializer(DependencyType::class),
                        String.serializer().list
                    ), 9
                )
                elemOutput.serializeObj(
                    this.replaceDependencies, obj.replaceDependencies, HashMapSerializer(
                        String.serializer(),
                        String.serializer()
                    ), 10
                )
                elemOutput.serializeObj(this.packageType, obj.packageType, EnumSerializer(PackageType::class), 11)
                elemOutput.serialize(this.transient, obj.transient, 12)
                elemOutput.serialize(this.version, obj.version, 13)
                if (this.fileName != obj.fileName) {
                    elemOutput.writeStringElementValue(serialClassDesc, 14, obj.fileName!!)
                }
                elemOutput.serialize(this.fileNameRegex, obj.fileNameRegex, 15)
                elemOutput.serializeObj(this.validMcVersions, obj.validMcVersions, String.serializer().set, 16)
                when {
                    provider.equalsIgnoreCase("CURSE") -> {
                        elemOutput.serialize(this.curseMetaUrl, obj.curseMetaUrl, 17)
                        elemOutput.serializeObj(
                            this.curseReleaseTypes,
                            obj.curseReleaseTypes,
                            EnumSerializer(FileType::class).set,
                            18
                        )
                        elemOutput.serializeObj(this.curseProjectID, obj.curseProjectID, ProjectID.Companion, 19)
                        elemOutput.serializeObj(this.curseFileID, obj.curseFileID, FileID.Companion, 20)
                    }
                    provider.equalsIgnoreCase("DIRECT") -> {
                        elemOutput.serialize(this.url, obj.url, 21)
                        elemOutput.serialize(this.useUrlTxt, obj.useUrlTxt, 22)
                    }
                    provider.equalsIgnoreCase("JENKINS") -> {
                        elemOutput.serialize(this.jenkinsUrl, obj.jenkinsUrl, 23)
                        elemOutput.serialize(this.job, obj.job, 24)
                        elemOutput.serialize(this.buildNumber, obj.buildNumber, 25)
                    }
                    provider.equalsIgnoreCase("LOCAL") -> {
                        elemOutput.serialize(this.fileSrc, obj.fileSrc, 26)
                    }
                    provider.equalsIgnoreCase("JSON") -> {
                        elemOutput.serialize(this.updateJson, obj.updateJson, 27)
                        elemOutput.serialize(this.updateChannel, obj.updateChannel, 28)
                        elemOutput.serialize(this.template, obj.template, 29)
                    }
                }
            }
            elemOutput.writeEnd(serialClassDesc)
        }

        private inline fun <reified T : Any> KOutput.serialize(default: T, actual: T, index: Int) {
            if (default != actual)
                when (actual) {
                    is String -> this.writeStringElementValue(serialClassDesc, index, actual)
                    is Int -> this.writeIntElementValue(serialClassDesc, index, actual)
                    is Boolean -> this.writeBooleanElementValue(serialClassDesc, index, actual)
                    else -> {
                        this.writeSerializableElementValue(serialClassDesc, index, T::class.serializer(), actual)
                    }
                }
        }

        private fun <T : Any?> KOutput.serializeObj(default: T, actual: T, saver: KSerialSaver<T>, index: Int) {
            if (default != actual) {
                this.writeSerializableElementValue(serialClassDesc, index, saver, actual)
            }
        }

        private val json = JSON(
            indented = true,
            updateMode = UpdateMode.BANNED,
            nonstrict = true,
            unquoted = true,
            indent = "  ",
            context = SerialContext().apply {
                registerSerializer(Side::class, Side.Companion)
            })
    }

    @Transient
    var optional: Boolean = feature != null

    @Transient
    val cleanId: String
        get() = id
            .replace('/', '-')
            .replace("[^\\w-]+".toRegex(), "")
    @Transient
    val serialFilename: String
        get() = "$cleanId.entry.hjson"

    fun serialize(sourceFolder: File) {
        val file = sourceFolder.resolve(folder).resolve("$cleanId.entry.hjson").absoluteFile
        file.absoluteFile.parentFile.mkdirs()
        file.writeText(json.stringify(this))
    }

    fun lock(block: LockEntry.() -> Unit): LockEntry {
        val lockEntry = LockEntry(
            provider = provider,
            id = id,
            useUrlTxt = useUrlTxt,
            fileName = fileName,
            side = side
        )
        lockEntry.name = name
        lockEntry.block()
        lockEntry.serialFile = File(lockEntry.suggestedFolder ?: folder).resolve("$cleanId.lock.hjson")
        return lockEntry
    }
}
