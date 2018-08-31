package voodoo.data.lock

import blue.endless.jankson.JsonObject
import blue.endless.jankson.impl.Marshaller
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.coroutines.runBlocking
import voodoo.data.Side
import voodoo.data.curse.CurseConstancts.PROXY_URL
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.getReified
import voodoo.provider.Provider
import voodoo.provider.ProviderBase
import voodoo.util.equalsIgnoreCase
import java.time.Instant

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class LockEntry(
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var provider: String = "",
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var id: String = "",
        var name: String = "",
        var fileName: String? = null,
        var side: Side = Side.BOTH,
        // CURSE
        var curseMetaUrl: String = PROXY_URL,
        var projectID: ProjectID = ProjectID.INVALID,
        var fileID: FileID = FileID.INVALID,
        // DIRECT
        var url: String = "",
        var useUrlTxt: Boolean = true,
        // JENKINS
        var jenkinsUrl: String = "",
        var job: String = "",
        var buildNumber: Int = -1,
        var fileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$",
        // JSON
        var updateJson: String = "",
        var jsonVersion: String = "",
        // LOCAL
        var fileSrc: String = ""
) {
    @JsonIgnore
    lateinit var parent: LockPack

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


    companion object {

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
                if(fileName != null) jsonObject["fileName"] = marshaller.serialize(fileName)
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
}