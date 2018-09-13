package voodoo.data.lock

import blue.endless.jankson.JsonObject
import blue.endless.jankson.impl.Marshaller
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerialSaver
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import kotlinx.serialization.UpdateMode
import kotlinx.serialization.internal.EnumSerializer
import kotlinx.serialization.json.JSON
import mu.KLogging
import voodoo.data.Side
import voodoo.data.curse.CurseConstancts.PROXY_URL
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.data.flat.Entry
import voodoo.getReified
import voodoo.provider.Provider
import voodoo.provider.ProviderBase
import voodoo.util.equalsIgnoreCase
import java.io.File
import java.time.Instant

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Serializable
data class LockEntry(
    @JsonInclude(JsonInclude.Include.ALWAYS)
    var provider: String = "",
    @JsonInclude(JsonInclude.Include.ALWAYS)
    var id: String = "",
    @Optional var name: String = "",
    @Optional var fileName: String? = null,
    @Optional var side: Side = Side.BOTH,
    // CURSE
    @Optional var curseMetaUrl: String = PROXY_URL,
    @Optional var projectID: ProjectID = ProjectID.INVALID,
    @Optional var fileID: FileID = FileID.INVALID,
    // DIRECT
    @Optional var url: String = "",
    @Optional var useUrlTxt: Boolean = true,
    // JENKINS
    @Optional var jenkinsUrl: String = "",
    @Optional var job: String = "",
    @Optional var buildNumber: Int = -1,
    @Optional var fileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$",
    // JSON
    @Optional var updateJson: String = "",
    @Optional var jsonVersion: String = "",
    // LOCAL
    @Optional var fileSrc: String = ""
) {
    @JsonIgnore
    @Transient
    lateinit var parent: LockPack

    /**
     * relative to src folder
     */
    @JsonIgnore
    @Transient
    lateinit var file: File

    @JsonIgnore
    fun provider(): ProviderBase = Provider.valueOf(provider).base

    @JsonIgnore
    fun name(): String = name.takeIf { it.isNotBlank() } ?: runBlocking { provider().generateName(this@LockEntry) }

    @JsonIgnore
    fun version(): String = runBlocking { provider().getVersion(this@LockEntry) }

    @JsonIgnore
    fun license(): String = runBlocking { provider().getLicense(this@LockEntry) }

    @JsonIgnore
    fun thumbnail(): String = runBlocking { provider().getThumbnail(this@LockEntry) }

    @JsonIgnore
    fun authors(): String = runBlocking { provider().getAuthors(this@LockEntry).joinToString(", ") }

    @JsonIgnore
    fun projectPage(): String = runBlocking { provider().getProjectPage(this@LockEntry) }

    @JsonIgnore
    fun releaseDate(): Instant? = runBlocking { provider().getReleaseDate(this@LockEntry) }

    @JsonIgnore
    fun isCurse(): Boolean = provider == Provider.CURSE.name

    @JsonIgnore
    fun isJenkins(): Boolean = provider == Provider.JENKINS.name

    @JsonIgnore
    fun isDirect(): Boolean = provider == Provider.DIRECT.name

    @JsonIgnore
    fun isJson(): Boolean = provider == Provider.JSON.name

    @JsonIgnore
    fun isLocal(): Boolean = provider == Provider.LOCAL.name

    @Serializer(forClass = LockEntry::class)
    companion object : KLogging() {
        override fun save(output: KOutput, obj: LockEntry) {
            println(serialClassDesc)
            val elemOutput = output.writeBegin(serialClassDesc)
            elemOutput.writeStringElementValue(serialClassDesc, 0, obj.provider)
            elemOutput.writeStringElementValue(serialClassDesc, 1, obj.id)
            elemOutput.writeStringElementValue(serialClassDesc, 2, obj.name)
            with(LockEntry(provider = obj.provider, id = obj.id)) {
                if (this.fileName != obj.fileName) {
                    elemOutput.writeStringElementValue(serialClassDesc, 3, obj.fileName!!)
                }
                elemOutput.serializeObj(this.side, obj.side, EnumSerializer(Side::class), 4)
                elemOutput.serialize(this.curseMetaUrl, obj.curseMetaUrl, 5)
                elemOutput.serializeObj(this.projectID, obj.projectID, ProjectID.Companion, 6)
                elemOutput.serializeObj(this.fileID, obj.fileID, FileID.Companion, 7)
                elemOutput.serialize(this.url, obj.url, 8)
                elemOutput.serialize(this.useUrlTxt, obj.useUrlTxt, 9)
                elemOutput.serialize(this.jenkinsUrl, obj.jenkinsUrl, 10)
                elemOutput.serialize(this.job, obj.job, 11)
                elemOutput.serialize(this.buildNumber, obj.buildNumber, 12)
                elemOutput.serialize(this.fileNameRegex, obj.fileNameRegex, 13)
                elemOutput.serialize(this.updateJson, obj.updateJson, 28)
                elemOutput.serialize(this.jsonVersion, obj.jsonVersion, 29)
                elemOutput.serialize(this.fileSrc, obj.fileSrc, 27)
            }
            output.writeEnd(serialClassDesc)
        }

        private inline fun <reified T : Any> KOutput.serialize(default: T, actual: T, index: Int) {
            if (default != actual)
                when (actual) {
                    is String -> this.writeStringElementValue(serialClassDesc, index, actual)
                    is Int -> this.writeIntElementValue(serialClassDesc, index, actual)
                    is Boolean -> this.writeBooleanElementValue(serialClassDesc, index, actual)
                }
        }

        private fun <T : Any?> KOutput.serializeObj(default: T, actual: T, saver: KSerialSaver<T>, index: Int) {
            if (default != actual) {
                this.writeElement(serialClassDesc, index)
                this.write(saver, actual)
            }
        }

        private val json = JSON(
            indented = true,
            updateMode = UpdateMode.BANNED,
            nonstrict = true,
            unquoted = true,
            indent = "  ",
            context = SerialContext().apply {
                registerSerializer(Side::class, Side)
            })

        fun loadEntry(file: File): LockEntry =
            json.parse<LockEntry>(file.readText().also { logger.info { "loading: $it" } })

        fun fromJson(jsonObject: JsonObject): LockEntry {
            return with(LockEntry()) {
                LockEntry(
                    provider = jsonObject.getReified("provider") ?: provider,
                    id = jsonObject.getReified("id") ?: id,
                    name = jsonObject.getReified("name") ?: name,
                    //rootFolder = jsonObject.getReified("rootFolder") ?: rootFolder,
                    fileName = jsonObject.getReified("fileName") ?: fileName,
                    side = jsonObject.getReified("side") ?: side,
                    curseMetaUrl = jsonObject.getReified("curseMetaUrl") ?: curseMetaUrl,
                    projectID = jsonObject.getReified("projectID") ?: projectID,
                    fileID = jsonObject.getReified("fileID") ?: fileID,
                    url = jsonObject.getReified("url") ?: url,
                    useUrlTxt = jsonObject.getReified("useUrlTxt") ?: useUrlTxt,
                    jenkinsUrl = jsonObject.getReified("jenkinsUrl") ?: jenkinsUrl,
                    job = jsonObject.getReified("job") ?: job,
                    buildNumber = jsonObject.getReified("buildNumber") ?: buildNumber,
                    fileNameRegex = jsonObject.getReified("fileNameRegex") ?: fileNameRegex,
                    updateJson = jsonObject.getReified("updateJson") ?: updateJson,
                    jsonVersion = jsonObject.getReified("jsonVersion") ?: jsonVersion,
                    fileSrc = jsonObject.getReified("fileSrc") ?: fileSrc
                )
            }
        }

        fun toJson(lockEntry: LockEntry, marshaller: Marshaller): JsonObject {
            val jsonObject = JsonObject()
            with(lockEntry) {
                jsonObject["provider"] = marshaller.serialize(provider)
                jsonObject["id"] = marshaller.serialize(id)
                jsonObject["name"] = marshaller.serialize(name)
                //jsonObject["rootFolder"] = marshaller.serialize(rootFolder)
                if (fileName != null) jsonObject["fileName"] = marshaller.serialize(fileName)
                jsonObject["side"] = marshaller.serialize(side)
                jsonObject["fileNameRegex"] = marshaller.serialize(fileNameRegex)
                when {
                    provider.equalsIgnoreCase("CURSE") -> {
                        jsonObject["curseMetaUrl"] = marshaller.serialize(curseMetaUrl)
                        jsonObject["projectID"] = marshaller.serialize(projectID)
                        jsonObject["fileID"] = marshaller.serialize(fileID)
                    }
                    provider.equalsIgnoreCase("DIRECT") -> {
                        jsonObject["url"] = marshaller.serialize(url)
                        jsonObject["useUrlTxt"] = marshaller.serialize(useUrlTxt)
                    }
                    provider.equalsIgnoreCase("JENKINS") -> {
                        jsonObject["jenkinsUrl"] = marshaller.serialize(jenkinsUrl)
                        jsonObject["job"] = marshaller.serialize(job)
                        jsonObject["buildNumber"] = marshaller.serialize(buildNumber)
                    }
                    provider.equalsIgnoreCase("LOCAL") -> {
                        jsonObject["fileSrc"] = marshaller.serialize(fileSrc)
                    }
                    provider.equalsIgnoreCase("JSON") -> {
                        jsonObject["updateJson"] = marshaller.serialize(updateJson)
                        jsonObject["jsonVersion"] = marshaller.serialize(jsonVersion)
                    }
                }
            }
            return jsonObject
        }
    }

    fun toDefaultJson(marshaller: Marshaller): JsonObject {
        return (marshaller.serialize(
            LockEntry(provider = this.provider)
        ) as JsonObject).apply {
            this.remove("provider")
            this.remove("id")
            this.remove("name")
        }
    }

    fun serialize(): String = json.stringify(this)

}