package voodoo.data.lock

import blue.endless.jankson.JsonObject
import blue.endless.jankson.impl.Marshaller
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import voodoo.data.Side
import voodoo.data.curse.CurseConstancts.PROXY_URL
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
        var name: String = "",
        //var folder: String = "mods",
        var fileName: String? = null,
        var side: Side = Side.BOTH,
        // CURSE
        var curseMetaUrl: String = PROXY_URL,
        var projectID: Int = -1,
        var fileID: Int = -1,
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
    private fun providerBase(): ProviderBase = Provider.valueOf(provider).base

    @JsonIgnore
    fun version(): String = providerBase().getVersion(this)

    @JsonIgnore
    fun license(): String = providerBase().getLicense(this)

    @JsonIgnore
    fun thumbnail(): String = providerBase().getThumbnail(this)

    @JsonIgnore
    fun authors(): String = providerBase().getAuthors(this).joinToString(", ")

    @JsonIgnore
    fun projectPage(): String = providerBase().getProjectPage(this)

    @JsonIgnore
    fun releaseDate(): Instant? = providerBase().getReleaseDate(this)

    @JsonIgnore
    fun isCurse(): Boolean = provider == Provider.CURSE.name

    @JsonIgnore
    fun isJenkins(): Boolean = provider == Provider.JENKINS.name

    @JsonIgnore
    fun isDirect(): Boolean = provider == Provider.DIRECT.name

    @JsonIgnore
    fun isJson(): Boolean = provider == Provider.JSON.name

    @JsonIgnore
    fun sLocal(): Boolean = provider == Provider.LOCAL.name


    companion object {

        fun fromJson(jsonObject: JsonObject): LockEntry {
            return with(LockEntry()) {
                LockEntry(
                        provider = jsonObject.getReified("provider") ?: provider,
                        name = jsonObject.getReified("name") ?: name,
                        //folder = jsonObject.getReified("folder") ?: folder,
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
                jsonObject["name"] = marshaller.serialize(name)
                //jsonObject["folder"] = marshaller.serialize(folder)
                jsonObject["fileName"] = marshaller.serialize(fileName)
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
            this.remove("name")
        }
    }
}