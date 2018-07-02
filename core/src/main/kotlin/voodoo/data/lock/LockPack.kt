package voodoo.data.lock

import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonObject
import blue.endless.jankson.impl.Marshaller
import com.fasterxml.jackson.annotation.JsonIgnore
import voodoo.data.UserFiles
import voodoo.data.sk.Launch
import voodoo.data.sk.SKFeature
import voodoo.fromJson
import voodoo.getList
import voodoo.getReified
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

data class LockPack(
        val name: String = "",
        val title: String = "",
        val version: String = "1.0",
        val authors: List<String> = emptyList(),
        val mcVersion: String = "",
        val forge: Int = -1,
        val launch: Launch = Launch(),
        var userFiles: UserFiles = UserFiles(),
        var localDir: String = "local",
        var sourceDir: String = name,
        val features: List<SKFeature> = emptyList()
) {
    @JsonIgnore
    var entries: List<LockEntry> = emptyList()

    init {
        scanEntries()

        entries.forEach { it.parent = this }
    }

    fun scanEntries() {
        //TODO: walk folders and get all LockEntries
    }

    companion object {
        fun fromJson(jsonObject: JsonObject): LockPack {
            return with(LockPack()) {
                LockPack(
                        name = jsonObject.getReified("name") ?: name,
                        title = jsonObject.getReified("title") ?: title,
                        version = jsonObject.getReified("version") ?: version,
                        authors = jsonObject.getList("authors") ?: authors,
                        mcVersion = jsonObject.getReified("mcVersion") ?: mcVersion,
                        forge = jsonObject.getReified("forge") ?: forge,
                        launch = jsonObject.getReified("launch") ?: launch,
                        userFiles = jsonObject.getReified("userFiles") ?: userFiles,
                        localDir = jsonObject.getReified("localDir") ?: localDir,
                        sourceDir = jsonObject.getReified("sourceDir") ?: sourceDir,
                        features = jsonObject.getList("features") ?: features
                )
            }
        }
        fun toJson(lockpack: LockPack, marshaller: Marshaller): JsonObject {
            val jsonObject = JsonObject()
            with(lockpack) {
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
                jsonObject["features"] = marshaller.serialize(features)
            }
            return jsonObject
        }
    }

    val entriesMapping: MutableMap<String, Pair<LockEntry, File>> = mutableMapOf()

    fun loadEntries(folder: File, jankson: Jankson) {
        val srcDir = folder.resolve(sourceDir)
        srcDir.walkTopDown()
                .filter {
                    it.isFile && it.name.endsWith(".lock.json")
                }
                .forEach {
                    val entryJsonObj = jankson.load(it)
                    val lockEntry: LockEntry = jankson.fromJson(entryJsonObj)
                    entriesMapping[lockEntry.name] = Pair(lockEntry, it)
                }
    }
}