package voodoo.data.flat

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UpdateMode
import kotlinx.serialization.json.Json
import mu.KLogging
import voodoo.data.OptionalData
import voodoo.data.Side
import voodoo.data.curse.CurseConstants.PROXY_URL
import voodoo.data.curse.DependencyType
import voodoo.data.curse.FileID
import voodoo.data.curse.FileType
import voodoo.data.curse.PackageType
import voodoo.data.curse.ProjectID
import voodoo.data.lock.LockEntry
import voodoo.data.provider.UpdateChannel
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

@Serializable
data class Entry(
    val provider: String,
    var id: String,
    @Optional var name: String? = null, // TODO add `by provider.getDisplayname(this)`
    @Optional var folder: String = "mods",
    @Optional var description: String? = null,
    @Optional var optionalData: OptionalData? = null,
    @Optional var side: Side = Side.BOTH,
    @Optional var websiteUrl: String = "",
    // TODO dependency declarations
    @Optional var dependencies: MutableMap<DependencyType, List<String>> = mutableMapOf(),
    @Optional var replaceDependencies: Map<ProjectID, ProjectID> = mapOf(),
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
    @Optional var curseReleaseTypes: Set<FileType> = setOf(FileType.Release, FileType.Beta),
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

    companion object : KLogging() {
        private val json = Json(
            indented = true,
            updateMode = UpdateMode.BANNED,
            strictMode = false,
            unquoted = true,
            indent = "  ",
            encodeDefaults = false
//            context = SerialContext().apply {
//                registerSerializer(Side::class, Side.Companion)
//            }
        )
    }

    @Transient
    var optional: Boolean = optionalData != null

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
        file.writeText(json.stringify(Entry.serializer(), this))
    }

    fun lock(block: LockEntry.() -> Unit): LockEntry {
        val lockEntry = LockEntry(
            provider = provider,
            useUrlTxt = useUrlTxt,
            fileName = fileName,
            side = side,
            description = description,
            optionalData = optionalData,
            dependencies = dependencies.toMap(),
            updateChannel = updateChannel,
            updateJson = updateJson,
            nameField = name
        )
        lockEntry.id = id
        lockEntry.block()
        lockEntry.folder = File(lockEntry.suggestedFolder ?: folder)
        // TODO: calculate serialiFile based on id and reverse
//        lockEntry.serialFile = File(lockEntry.suggestedFolder ?: folder).resolve("$cleanId.lock.hjson")
        return lockEntry
    }
}
