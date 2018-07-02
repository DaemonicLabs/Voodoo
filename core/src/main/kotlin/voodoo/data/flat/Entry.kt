package voodoo.data.flat

import blue.endless.jankson.JsonObject
import blue.endless.jankson.impl.Marshaller
import com.fasterxml.jackson.annotation.JsonIgnore
import voodoo.data.Side
import voodoo.data.curse.CurseConstancts.PROXY_URL
import voodoo.data.curse.DependencyType
import voodoo.data.curse.FileType
import voodoo.data.curse.PackageType
import voodoo.data.provider.UpdateChannel
import voodoo.getList
import voodoo.getReified
import voodoo.util.equalsIgnoreCase

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

//@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Entry(
        //@JsonInclude(JsonInclude.Include.ALWAYS)
        val provider: String,
        var name: String = "",
        var folder: String = "mods",
        var comment: String = "",
        var description: String = "",
        var feature: EntryFeature? = null,
        var side: Side = Side.BOTH,
        var websiteUrl: String = "",
        var dependencies: MutableMap<DependencyType, List<String>> = mutableMapOf(),
        //@JsonInclude(JsonInclude.Include.ALWAYS)
//        var optional: Boolean = feature != null,
        var packageType: PackageType = PackageType.MOD,
        var transient: Boolean = false, // this entry got added as dependency for something else
        var version: String = "", //TODO: use regex only ?
        var fileName: String? = null,
        var fileNameRegex: String = when {
            provider.equals("CURSE", true) -> ".*(?<!-deobf\\.jar)\$"
            provider.equals("JENKINS", true) -> ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$"
            else -> ".*"
        },
        var validMcVersions: Set<String> = setOf(),
        // CURSE
        var curseMetaUrl: String = PROXY_URL,
        var curseReleaseTypes: Set<FileType> = setOf(FileType.RELEASE, FileType.BETA),
        var curseOptionalDependencies: Boolean = false,
        // DIRECT
        var url: String = "",
        var useUrlTxt: Boolean = true,
        // JENKINS
        var jenkinsUrl: String = "",
        var job: String = "",
        var buildNumber: Int = -1,
        // LOCAL
        var fileSrc: String = "",
        // UPDATE-JSON
        var updateJson: String = "",
        var updateChannel: UpdateChannel = UpdateChannel.RECOMMENDED,
        var template: String = ""
) {
    @JsonIgnore
    var optional: Boolean = feature != null

    fun toDefaultJson(marshaller: Marshaller): JsonObject {
        return (marshaller.serialize(
                Entry(provider = this.provider)
        ) as JsonObject).apply {
            this.remove("provider")
            if (feature != null)
                this["feature"] = EntryFeature.toDefaultJson(feature, marshaller)
        }
    }

    companion object {
        fun toJson(entry: Entry, marshaller: Marshaller): JsonObject {
            val jsonObj = JsonObject()
            with(entry) {
                jsonObj["provider"] = marshaller.serialize(provider)
                jsonObj["name"] = marshaller.serialize(name)
                jsonObj["comment"] = marshaller.serialize(comment)
                jsonObj["description"] = marshaller.serialize(description)
                jsonObj["feature"] = marshaller.serialize(feature)
                jsonObj["side"] = marshaller.serialize(side)
                jsonObj["websiteUrl"] = marshaller.serialize(websiteUrl)
                jsonObj["dependencies"] = marshaller.serialize(dependencies)
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
                        jsonObj["curseReleaseTypes"] = marshaller.serialize(curseReleaseTypes)
                        jsonObj["curseOptionalDependencies"] = marshaller.serialize(curseOptionalDependencies)
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
            val provider: String = jsonObj.getReified("provider")!!
            return with(Entry(provider = provider)) {
                Entry(
                        provider = provider,
                        name = jsonObj.getReified("name") ?: name,
                        // TODO:  folder
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
                                else -> null
                            }
                        } ?: dependencies,
                        //jsonObj.getReified("dependencies") ?: dependencies,
//                        optional = jsonObj.getReified("optional") ?: optional,
                        packageType = jsonObj.getReified("packageType") ?: packageType,
                        transient = jsonObj.getReified("transient") ?: transient,
                        version = jsonObj.getReified("version") ?: version,
                        fileName = jsonObj.getReified("fileName") ?: fileName,
                        fileNameRegex = jsonObj.getReified("fileNameRegex") ?: fileNameRegex,
                        validMcVersions = jsonObj.getList<String>("validMcVersions")?.toSet() ?: validMcVersions,
                        //CURSE
                        curseMetaUrl = jsonObj.getReified("curseMetaUrl") ?: curseMetaUrl,
                        curseReleaseTypes = jsonObj.getList<FileType>("curseReleaseTypes")?.toSet() ?: curseReleaseTypes,
                        curseOptionalDependencies = jsonObj.getReified("curseOptionalDependencies")
                                ?: curseOptionalDependencies,
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
                    //jsonObj.getReified<String>("name")?.let { name = it }
                }
            }
        }


    }
}
