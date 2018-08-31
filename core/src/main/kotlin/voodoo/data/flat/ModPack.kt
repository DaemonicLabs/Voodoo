package voodoo.data.flat

import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonObject
import blue.endless.jankson.impl.Marshaller
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import mu.KLogging
import voodoo.data.Side
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
        var id: String,
        var title: String = "",
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var version: String = "1.0",
        var icon: String = "icon.png",
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

    companion object : KLogging() {
        fun toJson(modpack: ModPack, marshaller: Marshaller): JsonObject {
            val jsonObject = JsonObject()
            with(modpack) {
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
            }
            return jsonObject
        }

        fun fromJson(jsonObj: JsonObject): ModPack {

            val name: String = jsonObj.getReified("id")!!
            return with(ModPack(name)) {
                ModPack(
                        id = name,
                        title = jsonObj.getReified("title") ?: title,
                        version = jsonObj.getReified("version") ?: version,
                        icon = jsonObj.getReified("icon") ?: icon,
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
        return (marshaller.serialize(ModPack(id)) as JsonObject).apply {
            this.remove("id")
        }
    }

    //TODO: move file into ModPack ad LockPack as lateinit
    val entriesSet: MutableSet<Entry> = mutableSetOf()
    val lockEntrySet: MutableSet<LockEntry> = mutableSetOf()

    fun addEntry(entry: Entry, file: File, dependency: Boolean = false) {
        if (entry.id.isBlank()) {
            logger.error("invalid: $entry")
            return
        }

        entry.file = file

        val existingEntry = entriesSet.find { it.id == entry.id }
        if (existingEntry == null) {
            logger.info("new entry ${entry.id}")

            entriesSet += entry
        } else {
            if (entry == existingEntry) {
                return
            }

            logger.info("duplicate entry $entry.id")

            if (!dependency && !existingEntry.transient) {
                throw IllegalStateException("duplicate entries: ${existingEntry.file} and ${existingEntry.file}")
            }

            // TODO: make some util code to merge Entries and their JsonObj
            existingEntry.side += entry.side
            if (existingEntry.feature == null) {
                existingEntry.feature = entry.feature
            }
            if (existingEntry.description.isBlank()) {
                existingEntry.description = entry.description
            }
        }
    }

    fun loadEntries(folder: File, jankson: Jankson) {
        val srcDir = folder.resolve(sourceDir)
        srcDir.walkTopDown()
                .filter {
                    it.isFile && it.name.endsWith(".entry.hjson")
                }
                .forEach { file ->
                    val entryJsonObj = jankson.load(file)
                    val entry: Entry = jankson.fromJson(entryJsonObj)
                    addEntry(entry, file, false)
                }
    }

    //TODO: call from LockPack ?
    fun loadLockEntries(folder: File, jankson: Jankson) {
        val srcDir = folder.resolve(sourceDir)
        srcDir.walkTopDown()
                .filter {
                    it.isFile && it.name.endsWith(".lock.json")
                }
                .forEach {
                    val entryJsonObj = jankson.load(it)
                    val lockEntry: LockEntry = jankson.fromJson(entryJsonObj)
                    lockEntry.file = it
                    lockEntrySet[lockEntry.id] = lockEntry
                }
    }

    fun writeEntries(folder: File, jankson: Jankson) {
        entriesSet.forEach { entry ->
            val folder = folder.resolve(sourceDir).resolve(entry.folder)
            //TODO: calculate filename in Entry
            val filename = entry.id
                    .replace('/', '-')
                    .replace("[^\\w-]+".toRegex(), "")
            val targetFile = folder.resolve("$filename.entry.hjson")
            //TODO: only override folder if it was uninitialized before
            entry.file = targetFile
            entry.serialize(jankson)
        }
    }

    fun writeLockEntries(folder: File, jankson: Jankson) {
        lockEntrySet.forEach { lockEntry ->
            logger.info("saving: ${lockEntry.id} , file: ${lockEntry.file} , entry: $lockEntry")

            val folder = lockEntry.file.absoluteFile.parentFile

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

    fun lock(): LockPack {
        return LockPack(
                id = id,
                title = title,
                version = version,
                icon = icon,
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

operator fun MutableSet<LockEntry>.set(id: String, entry: LockEntry) {
    this.findByid(id)?.let {
        this -= it
    }
    this += entry
}

fun MutableSet<Entry>.findByid(id: String) = this.find { it.id == id }
fun MutableSet<LockEntry>.findByid(id: String) = this.find { it.id == id }