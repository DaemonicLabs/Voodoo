package voodoo.data.lock

import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonObject
import blue.endless.jankson.impl.Marshaller
import com.fasterxml.jackson.annotation.JsonIgnore
import voodoo.data.Side
import voodoo.data.UserFiles
import voodoo.data.flat.ModPack
import voodoo.data.sk.Launch
import voodoo.data.sk.SKFeature
import voodoo.fromJson
import voodoo.getList
import voodoo.getReified
import voodoo.markdownTable
import voodoo.util.blankOr
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

data class LockPack(
        val id: String = "",
        val title: String = "",
        val version: String = "1.0",
        val icon: String = "icon.png",
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
                        icon = jsonObject.getReified("icon") ?: icon,
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
                jsonObject["icon"] = marshaller.serialize(icon)
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
//        private set

    val sourceFolder: File
        get() = rootFolder.resolve(sourceDir)
    val localFolder: File
        get() = rootFolder.resolve(localDir)
    val iconFile: File
        get() = rootFolder.resolve(icon)

    @JsonIgnore
    val entrySet: MutableSet<LockEntry> = mutableSetOf()


    fun loadEntries(rootFolder: File = this.rootFolder, jankson: Jankson) {
        this.rootFolder = rootFolder
        val srcDir = rootFolder.resolve(sourceDir)
        srcDir.walkTopDown()
                .filter {
                    it.isFile && it.name.endsWith(".lock.json")
                }
                .forEach {
                    val relFile = it.relativeTo(srcDir)
                    val entryJsonObj = jankson.load(it)
                    val lockEntry: LockEntry = jankson.fromJson(entryJsonObj)
                    lockEntry.file = relFile
                    addOrMerge(lockEntry) { dupl, newEntry -> newEntry}
                }
    }


    fun writeLockEntries(jankson: Jankson) {
        entrySet.forEach { lockEntry ->
            ModPack.logger.info("saving: ${lockEntry.id} , file: ${lockEntry.file} , entry: $lockEntry")

            val folder = sourceFolder.resolve(lockEntry.file).absoluteFile.parentFile

            val targetFolder = if (folder.toPath().none { it.toString() == "_CLIENT" || it.toString() == "_SERVER" }) {
                when (lockEntry.side) {
                    Side.CLIENT -> {
                        folder.resolve("_CLIENT")
                    }
                    Side.SERVER -> {
                        folder.resolve("_SERVER")
                    }
                    Side.BOTH -> folder
                }
            } else folder

            targetFolder.mkdirs()

            val defaultJson = lockEntry.toDefaultJson(jankson.marshaller)
            val lockJson = jankson.toJson(lockEntry) as JsonObject
            val delta = lockJson.getDelta(defaultJson)

            val targetFile = targetFolder.resolve(lockEntry.file.name)

            targetFile.writeText(delta.toJson(true, true).replace("\t", "  "))
        }
    }

    fun title() = title.blankOr ?: id

    val report: String
        get() = markdownTable(header = "Title" to this.title(), content = listOf(
                        "ID" to "`$id`",
                        "Pack Version" to "`$version`",
                        "MC Version" to "`$mcVersion`",
                        "Author" to "`${authors.joinToString(", ")}`",
                        "Icon" to "<img src=\"$icon\" alt=\"icon\" style=\"max-height: 128px;\"/>"
                ))

    fun findEntryById(id: String) = entrySet.find { it.id == id }

    operator fun MutableSet<LockEntry>.set(id: String, entry: LockEntry) {
        findEntryById(id)?.let {
            this -= it
        }
        this += entry
    }

    fun addOrMerge(entry: LockEntry, mergeOp: (LockEntry, LockEntry) -> LockEntry): LockEntry {
        val result = findEntryById(entry.id)?.let {
            entrySet -= it
            mergeOp(it, entry)
        } ?: entry
        entrySet += result
        return result
    }
}
