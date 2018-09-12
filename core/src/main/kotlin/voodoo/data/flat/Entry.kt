package voodoo.data.flat

import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonObject
import blue.endless.jankson.impl.Marshaller
import blue.endless.jankson.impl.SyntaxError
import com.fasterxml.jackson.annotation.JsonIgnore
import kotlinx.serialization.*
import kotlinx.serialization.Optional
import kotlinx.serialization.json.JSON
import mu.KLogging
import voodoo.data.Side
import voodoo.data.curse.*
import voodoo.data.curse.CurseConstancts.PROXY_URL
import voodoo.data.provider.UpdateChannel
import voodoo.getList
import voodoo.getMap
import voodoo.getReified
import voodoo.util.equalsIgnoreCase
import java.io.File
import java.util.*
import kotlin.jvm.Transient

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

//@JsonInclude(JsonInclude.Include.NON_DEFAULT)
//@Serializable
data class Entry(
        //@JsonInclude(JsonInclude.Include.ALWAYS)
        val provider: String,
        var id: String,
        @Optional var name: String = "",  // TODO add `by provider.getDisplayname(this)`
        @Optional var folder: String = "mods",
        @Optional var comment: String = "",
        @Optional var description: String = "",
        @Optional var feature: EntryFeature? = null,
        @Optional var side: Side = Side.BOTH,
        @Optional var websiteUrl: String = "",
        @Optional var dependencies: MutableMap<DependencyType, List<String>> = mutableMapOf(),
        @Optional var replaceDependencies: Map<String, String> = mapOf(),
        //@JsonInclude(JsonInclude.Include.ALWAYS)
//        @Optional var optional: Boolean = feature != null,
        @Optional var packageType: PackageType = PackageType.MOD,
        @Optional var transient: Boolean = false, // this entry got added as dependency for something else
        @Optional var version: String = "", //TODO: use regex only ?
        @Optional var fileName: String? = null,
        @Optional var fileNameRegex: String = when {
            provider.equals("CURSE", true) -> ".*(?<!-deobf\\.jar)\$"
            provider.equals("JENKINS", true) -> ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$"
            else -> ".*"
        },
        @Optional var validMcVersions: Set<String> = setOf(),
        // CURSE
        @Optional var curseMetaUrl: String = PROXY_URL,
        @Optional var curseReleaseTypes: Set<FileType> = setOf(FileType.RELEASE, FileType.BETA),
        @Optional var curseOptionalDependencies: Boolean = false,
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
    @JsonIgnore
    @Transient
    var optional: Boolean = feature != null

    /**
     * abssolute file
     */
    @JsonIgnore
    @Transient
    lateinit var file: File

    fun serialize(jankson: Jankson) {
        file.absoluteFile.parentFile.mkdirs()
        val json = jankson.marshaller.serialize(this)//.toJson(true, true)
        if (json is JsonObject) {
            val defaultJson = this.toDefaultJson(jankson.marshaller)
            val delta = json.getDelta(defaultJson)
            file.writeText(delta.toJson(true, true).replace("\t", "  "))
        }
    }

    fun toDefaultJson(marshaller: Marshaller): JsonObject {
        return (marshaller.serialize(
                Entry(provider = this.provider, id = this.id)
        ) as JsonObject).apply {
            this.remove("provider")
            this.remove("id")
            if (feature != null)
                this["feature"] = EntryFeature.toDefaultJson(feature, marshaller)
        }
    }

//    @Serializer(forClass = Entry::class)
    companion object: KLogging() {
//        override fun load(input: KInput): Entry {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }
//
//        override fun save(output: KOutput, obj: Entry) {
//            output.write
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }

        val json = JSON(indented = true, context = SerialContext())

        fun toJson(entry: Entry, marshaller: Marshaller): JsonObject {
            val jsonObj = JsonObject()
            with(entry) {
                jsonObj["provider"] = marshaller.serialize(provider)
                jsonObj["id"] = marshaller.serialize(id)
                jsonObj["name"] = marshaller.serialize(name)
                jsonObj["comment"] = marshaller.serialize(comment)
                jsonObj["description"] = marshaller.serialize(description)
                jsonObj["feature"] = marshaller.serialize(feature)
                jsonObj["side"] = marshaller.serialize(side)
                jsonObj["websiteUrl"] = marshaller.serialize(websiteUrl)
                jsonObj["dependencies"] = marshaller.serialize(dependencies)
                jsonObj["replaceDependencies"] = marshaller.serialize(replaceDependencies)
//                jsonObj["optional"] = marshaller.serialize(optional)
                jsonObj["packageType"] = marshaller.serialize(packageType)
                jsonObj["transient"] = marshaller.serialize(transient)
                jsonObj["version"] = marshaller.serialize(version)
                jsonObj["fileName"] = marshaller.serialize(fileName)
                jsonObj["fileNameRegex"] = marshaller.serialize(fileNameRegex)
                jsonObj["validMcVersions"] = marshaller.serialize(validMcVersions)
                when {
                    provider.equalsIgnoreCase("CURSE") -> {
                        jsonObj["curseMetaUrl"] = marshaller.serialize(curseMetaUrl)
                        jsonObj["curseReleaseTypes"] = marshaller.serialize(curseReleaseTypes.toSortedSet())
                        jsonObj["curseOptionalDependencies"] = marshaller.serialize(curseOptionalDependencies)
                        if(curseProjectID.valid) jsonObj["curseProjectID"] = marshaller.serialize(curseProjectID)
                        if(curseFileID.valid) jsonObj["curseFileID"] = marshaller.serialize(curseFileID)
                    }
                    provider.equalsIgnoreCase("DIRECT") -> {
                        jsonObj["url"] = marshaller.serialize(url)
                        jsonObj["useUrlTxt"] = marshaller.serialize(useUrlTxt)
                    }
                    provider.equalsIgnoreCase("JENKINS") -> {
                        jsonObj["jenkinsUrl"] = marshaller.serialize(jenkinsUrl)
                        jsonObj["job"] = marshaller.serialize(job)
                        jsonObj["buildNumber"] = marshaller.serialize(buildNumber)
                    }
                    provider.equalsIgnoreCase("LOCAL") -> {
                        jsonObj["fileSrc"] = marshaller.serialize(fileSrc)
                    }
                    provider.equalsIgnoreCase("JSON") -> {
                        jsonObj["updateJson"] = marshaller.serialize(updateJson)
                        jsonObj["updateChannel"] = marshaller.serialize(updateChannel)
                        jsonObj["template"] = marshaller.serialize(template)
                    }
                }
            }

            return jsonObj
        }

        fun fromJson(jsonObj: JsonObject): Entry {
            val provider: String = jsonObj.getReified("provider") ?: throw NullPointerException("missing field: provider")
            val id: String = jsonObj.getReified("id") ?: throw NullPointerException("missing field: id")
            return with(Entry(provider = provider, id = id)) {
                Entry(
                        provider = provider,
                        id = id,
                        name = jsonObj.getReified("name") ?: name,
                        folder = jsonObj.getReified("rootFolder") ?: folder,
                        comment = jsonObj.getReified("comment") ?: comment,
                        description = jsonObj.getReified("description") ?: description,
                        feature = jsonObj.getReified("feature") ?: feature,
                        side = jsonObj.getReified("side") ?: side,
                        websiteUrl = jsonObj.getReified("websiteUrl") ?: websiteUrl,
                        dependencies = jsonObj["dependencies"]?.let { mapObj ->
                            when(mapObj) {
                                is JsonObject -> {
                                    mapObj.mapValues { (key, _) ->
                                        mapObj.getList<String>(key) ?: emptyList()
                                    }.mapKeys { (key, _) ->
                                        DependencyType.valueOf(key.toUpperCase())
                                    }.toMutableMap()
                                }
                                else -> throw SyntaxError("${mapObj::class.simpleName} is not allowed in dependencies")
                            }
                        } ?: dependencies,
                        //jsonObj.getReified("dependencies") ?: dependencies,
                        replaceDependencies = jsonObj.getMap<String>("replaceDependencies") ?: replaceDependencies,
//                        optional = jsonObj.getReified("optional") ?: optional,
                        packageType = jsonObj.getReified("packageType") ?: packageType,
                        transient = jsonObj.getReified("transient") ?: transient,
                        version = jsonObj.getReified("version") ?: version,
                        fileName = jsonObj.getReified("fileName") ?: fileName,
                        fileNameRegex = jsonObj.getReified("fileNameRegex") ?: fileNameRegex,
                        validMcVersions = jsonObj.getList<String>("validMcVersions")?.toSet() ?: validMcVersions,
                        //CURSE
                        curseMetaUrl = jsonObj.getReified("curseMetaUrl") ?: curseMetaUrl,
                        curseReleaseTypes = jsonObj.getList<FileType>("curseReleaseTypes")?.toSortedSet() ?: curseReleaseTypes,
                        curseOptionalDependencies = jsonObj.getReified("curseOptionalDependencies")
                                ?: curseOptionalDependencies,
                        curseProjectID = jsonObj.getReified("curseProjectID") ?: curseProjectID,
                        curseFileID = jsonObj.getReified("curseFileID") ?: curseFileID,
                        // DIRECT
                        url = jsonObj.getReified("url") ?: url,
                        useUrlTxt = jsonObj.getReified("useUrlTxt") ?: useUrlTxt,
                        // JENKINS
                        jenkinsUrl = jsonObj.getReified("jenkinsUrl") ?: jenkinsUrl,
                        job = jsonObj.getReified("job") ?: job,
                        buildNumber = jsonObj.getReified("buildNumber") ?: buildNumber,
                        //LOCAL
                        fileSrc = jsonObj.getReified("fileSrc") ?: fileSrc,
                        //UPDATE JSON
                        updateJson = jsonObj.getReified("updateJson") ?: updateJson,
                        updateChannel = jsonObj.getReified("updateChannel") ?: updateChannel,
                        template = jsonObj.getReified("template") ?: template
                ).apply {
                    //jsonObj.getReified<String>("id")?.let { id = it }
                }
            }
        }


    }
}
