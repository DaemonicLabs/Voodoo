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
        val id: String = "",
        val title: String = "",
        val version: String = "1.0",
        val authors: List<String> = emptyList(),
        val mcVersion: String = "",
        val forge: Int = -1,
        val launch: Launch = Launch(),
        var userFiles: UserFiles = UserFiles(),
        var localDir: String = "local",
        var sourceDir: String = id, //"src-$id",
        val features: List<SKFeature> = emptyList()
) {
    companion object {
        fun fromJson(jsonObject: JsonObject): LockPack {
            return with(LockPack()) {
                LockPack(
                        id = jsonObject.getReified("id") ?: id,
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
                jsonObject["id"] = marshaller.serialize(id)
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

    @JsonIgnore
    lateinit var rootFolder: File
        private set

    val sourceFolder: File
        get() = rootFolder.resolve(sourceDir)
    val localFolder: File
        get() = rootFolder.resolve(localDir)

    @JsonIgnore
    val entriesMapping: MutableMap<String, Pair<LockEntry, File>> = mutableMapOf()


    fun loadEntries(rootFolder: File = this.rootFolder, jankson: Jankson) {
        this.rootFolder = rootFolder
        sourceFolder.walkTopDown()
                .filter {
                    it.isFile && it.name.endsWith(".lock.json")
                }
                .forEach {
                    val entryJsonObj = jankson.load(it)
                    val lockEntry: LockEntry = jankson.fromJson(entryJsonObj)
                    lockEntry.parent = this
                    entriesMapping[lockEntry.id] = Pair(lockEntry, it.relativeTo(sourceFolder))
//                    logger.info("loaded ${lockEntry.id} ${it.relativeTo(srcDir).path}")
                }
    }

    val report: String
        get() = """# $title
            |ID: $id
            |version $version
            |MC Version $mcVersion
            |Authors ${authors.joinToString(", ")}
        """.trimMargin()
}