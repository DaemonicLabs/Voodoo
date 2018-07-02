package voodoo.data.flat

import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonObject
import blue.endless.jankson.impl.Marshaller
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import mu.KLogging
import voodoo.data.UserFiles
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.data.sk.Launch
import voodoo.data.sk.SKFeature
import voodoo.forge.Forge
import voodoo.fromJson
import voodoo.getList
import voodoo.getReified
import java.io.File


/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class ModPack(
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var name: String,
        var title: String = "",
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var version: String = "1.0",
        val authors: List<String> = emptyList(),
        var mcVersion: String = "",
        var forge: String = "recommended",
        //var forgeBuild: Int = -1,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        val launch: Launch = Launch(),
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var userFiles: UserFiles = UserFiles(),

        @JsonInclude(JsonInclude.Include.ALWAYS)
        var localDir: String = "local",
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var sourceDir: String = "src"
) {

    companion object: KLogging() {
        fun toJson(modpack: ModPack, marshaller: Marshaller): JsonObject {
            val jsonObject = JsonObject()
            with(modpack) {
                jsonObject["name"] = marshaller.serialize(name)
                jsonObject["title"] = marshaller.serialize(title)
                jsonObject["version"] = marshaller.serialize(version)
                jsonObject["authors"] = marshaller.serialize(authors)
                jsonObject["mcVersion"] = marshaller.serialize(mcVersion)
                jsonObject["forge"] = marshaller.serialize(forge)
                jsonObject["launch"] = marshaller.serialize(launch)
                jsonObject["userFiles"] = marshaller.serialize(userFiles)
                jsonObject["localDir"] = marshaller.serialize(localDir)
                jsonObject["sourceDir"] = marshaller.serialize(sourceDir)
            }
            return jsonObject
        }

        fun fromJson(jsonObj: JsonObject): ModPack {

            val name: String = jsonObj.getReified("name")!!
            return with(ModPack(name)) {
                ModPack(
                        name = name,
                        title = jsonObj . getReified ("title") ?: title,
                        version = jsonObj.getReified("version") ?: version,
                        authors = jsonObj.getList("authors") ?: authors,
                        mcVersion = jsonObj.getReified("mcVersion") ?: mcVersion,
                        forge = jsonObj.getReified("forge") ?: forge,
                        launch = jsonObj.getReified("launch") ?: launch,
                        userFiles = jsonObj.getReified("userFiles") ?: userFiles,
                        localDir = jsonObj.getReified("localDir") ?: localDir,
                        sourceDir = jsonObj.getReified("sourceDir") ?: sourceDir
                )
            }
        }
    }

    init {
//        if (versionCache.path == featureCache.path) {
//            versionCache.mkdirs()
//            featureCache.mkdirs()
//        }
//
//        if (versionCache.isDirectory)
//            versionCache = versionCache.resolve("versions.json")
//
//        logger.info("using version cache: $versionCache")
//        versions = versionCache.readJsonOrNull() ?: mutableMapOf()
//
//        if (featureCache.isDirectory)
//            featureCache = featureCache.resolve("features.json")
//
//        logger.info("using feature cache: $featureCache")
//        features = featureCache.readJsonOrNull() ?: mutableListOf()
    }

    @JsonIgnore
    val features: MutableList<SKFeature> = mutableListOf()

    fun toDefaultJson(marshaller: Marshaller): JsonObject {
        return (marshaller.serialize(ModPack(name)) as JsonObject).apply {
            this.remove("name")
        }
    }

    val entriesMapping: MutableMap<String, Triple<Entry, File, JsonObject>> = mutableMapOf()
    val versionsMapping: MutableMap<String, Pair<LockEntry, File>> = mutableMapOf()

    fun addEntry(entry: Entry, file: File, jsonObj: JsonObject, jankson: Jankson, dependency: Boolean = false) {
        if (entry.name.isBlank()) {
            logger.error("invalid: $entry")
            return
        }

        val duplTriple = entriesMapping[entry.name]
        if (duplTriple == null) {
            logger.info("new entry $entry.name")
            var tmpFile = file
            var tmpObj = jsonObj
            if(dependency) {
                entry.transient = true
                val filename = entry.name.replace("\\W+".toRegex(), "")
                tmpFile = file.absoluteFile.parentFile.resolve("$filename.entry.hjson")
                val json = jankson.toJson(entry) as JsonObject
                val defaultJson = entry.toDefaultJson(jankson.marshaller)
                tmpObj = json.getDelta(defaultJson)
            }

            entriesMapping[entry.name] = Triple(entry, tmpFile, tmpObj)
        } else {
            val (existingEntry, existingFile, existingJsonObj) = duplTriple

            if(entry == existingEntry && file == existingFile && jsonObj == existingJsonObj) {
                return
            }

            logger.info("duplicate entry $entry.name")

            if(!dependency && !existingEntry.transient) {
                throw IllegalStateException("duplicate entries: $existingFile and $file")
            }

            // TODO: make some util code to merge Entries and their JsonObj
            existingEntry.side += entry.side
            if (existingEntry.feature == null) {
                existingEntry.feature = entry.feature
                existingJsonObj["feature"] = jsonObj["feature"]
            }
            if (existingEntry.description.isBlank()) {
                existingEntry.description = entry.description
                existingJsonObj["description"] = jsonObj["description"]
            }
        }
    }

    fun loadEntries(folder: File, jankson: Jankson) {
        val srcDir = folder.resolve(sourceDir)
        srcDir.walkTopDown()
                .filter {
                    it.isFile && it.name.endsWith(".entry.hjson")
                }
                .forEach {
                    val entryJsonObj = jankson.load(it)
                    val entry: Entry = jankson.fromJson(entryJsonObj)
                    addEntry(entry, it, entryJsonObj, jankson, false)
                }
    }

    fun loadLockEntry(folder: File, jankson: Jankson) {
        val srcDir = folder.resolve(sourceDir)
        srcDir.walkTopDown()
                .filter {
                    it.isFile && it.name.endsWith(".lock.json")
                }
                .forEach {
                    val entryJsonObj = jankson.load(it)
                    val lockEntry: LockEntry = jankson.fromJson(entryJsonObj)
                    versionsMapping[lockEntry.name] = Pair(lockEntry, it)
                }
    }

    fun lock(): LockPack {
        return LockPack(
                name = name,
                title = title,
                version = version,
                authors = authors,
                mcVersion = mcVersion,
                forge = Forge.getForgeBuild(forge, mcVersion),
                launch = launch,
                userFiles = userFiles,
                localDir = localDir,
                sourceDir = sourceDir,
                features = features
        )
    }

}