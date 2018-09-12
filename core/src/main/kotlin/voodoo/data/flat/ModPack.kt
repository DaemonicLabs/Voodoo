package voodoo.data.flat

import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonObject
import blue.endless.jankson.impl.Marshaller
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.*
import kotlinx.serialization.Optional
import kotlinx.serialization.internal.PrimitiveDesc
import kotlinx.serialization.internal.SerialClassDescImpl
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
import java.util.*
import kotlin.system.exitProcess


/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Serializable
data class ModPack(
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var id: String,
        @Optional var title: String = "",
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @Optional var version: String = "1.0",
        @Optional var icon: String = "icon.png",
        @Optional val authors: List<String> = emptyList(),
        @Optional var mcVersion: String = "",
        @Optional var forge: String = "recommended",
        //var forgeBuild: Int = -1,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @Optional val launch: Launch = Launch(),
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @Optional var userFiles: UserFiles = UserFiles(),

        @JsonInclude(JsonInclude.Include.ALWAYS)
        @Optional var localDir: String = "local",
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @Optional var sourceDir: String = "src"
) {
//    @Serializer(forClass = ModPack::class)
    companion object : KLogging() { //, KSerializer<ModPack> {
//        override val serialClassDesc = object: SerialClassDescImpl("voodoo.data.flat.ModPack") {
//            init {
//                addElement("id")
//                addElement("title")
//                addElement("version")
//                addElement("icon")
//            }
//        }
//
//        override fun load(input: KInput): ModPack {
//            return ModPack(
//                    id = "test"
////                    id = input.readStringElementValue(PrimitiveDesc("id"), 0),
////                    title = input.readStringElementValue(PrimitiveDesc("title"), 1),
////                    icon = input.readStringElementValue(PrimitiveDesc("icon"), 2)
//            )
//        }
//
//        override fun save(output: KOutput, obj: ModPack) {
////            output.writeStringValue(obj.id)
//            output.writeStringElementValue(PrimitiveDesc("id"), 0, obj.id)
//
//            with(ModPack(obj.id)) {
//                if(this.title != obj.title)
////                    output.writeStringValue(obj.title)
//                    output.writeStringElementValue(PrimitiveDesc("title"), 1, obj.title)
//                if(this.version != obj.version)
////                    output.writeStringValue(obj.version)
//                    output.writeStringElementValue(PrimitiveDesc("title"), 2, obj.version)
//                if(this.icon != obj.icon)
////                    output.writeStringValue(obj.icon)
//                    output.writeStringElementValue(PrimitiveDesc("icon"), 2, obj.icon)
//            }
//        }

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
            val id: String = jsonObj.getReified("id")!!
            return with(ModPack(id)) {
                ModPack(
                        id = id,
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

    @Transient
    @JsonIgnore
    val features: MutableList<SKFeature> = mutableListOf()

    fun toDefaultJson(marshaller: Marshaller): JsonObject {
        return (marshaller.serialize(ModPack(id)) as JsonObject).apply {
            this.remove("id")
        }
    }

    //TODO: move file into ModPack ad LockPack as lateinit
    @Transient
    @JsonIgnore
    val entrySet: MutableSet<Entry> = Collections.synchronizedSet(mutableSetOf())
    @Transient
    @JsonIgnore
    val lockEntrySet: MutableSet<LockEntry> = Collections.synchronizedSet(mutableSetOf())

    fun addEntry(entry: Entry, file: File, dependency: Boolean = false) {
        if (entry.id.isBlank()) {
            logger.error("invalid: $entry")
            return
        }

        if (!file.isAbsolute) {
            logger.warn("file $file must be absolute")
            exitProcess(-1)
        }
        entry.file = file

        addOrMerge(entry) { existingEntry, newEntry ->
            if (newEntry == existingEntry) {
                return@addOrMerge newEntry
            }
            logger.info("duplicate entry $newEntry.id")

            if (!dependency && !existingEntry.transient) {
                throw IllegalStateException("duplicate entries: ${existingEntry.file} and ${existingEntry.file}")
            }

            // TODO: make some util code to merge Entries
            existingEntry.side += newEntry.side
            if (existingEntry.feature == null) {
                existingEntry.feature = newEntry.feature
            }
            if (existingEntry.description.isBlank()) {
                existingEntry.description = newEntry.description
            }

            existingEntry
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
                    addEntry(entry, file.absoluteFile, false)
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
                    val relFile = it.relativeTo(srcDir)
                    val entryJsonObj = jankson.load(it)
                    val lockEntry: LockEntry = jankson.fromJson(entryJsonObj)
                    lockEntry.file = relFile
                    addOrMerge(lockEntry) { dupl, newEntry -> newEntry }
                }
    }

    fun writeEntries(folder: File, jankson: Jankson) {
        entrySet.forEach { entry ->
            val folder = folder.resolve(sourceDir).resolve(entry.folder)
            //TODO: calculate filename in Entry
            val filename = entry.id
                    .replace('/', '-')
                    .replace("[^\\w-]+".toRegex(), "")
            val targetFile = folder.resolve("$filename.entry.hjson").absoluteFile
            //TODO: only override folder if it was uninitialized before
            entry.file = targetFile
            entry.serialize(jankson)
        }
    }

    fun writeLockEntries(folder: File, jankson: Jankson) {
        lockEntrySet.forEach { lockEntry ->
            logger.info("saving: ${lockEntry.id} , file: ${lockEntry.file} , entry: $lockEntry")

            val folder = folder.resolve(lockEntry.file).absoluteFile.parentFile

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

    fun findEntryById(id: String) = entrySet.find { it.id == id }
    fun addOrMerge(entry: Entry, mergeOp: (Entry, Entry) -> Entry): Entry {
        synchronized(entrySet) {
            val result = entrySet.find { it.id == entry.id }?.let {
                entrySet -= it
                mergeOp(it, entry)
            } ?: entry
            entrySet += result
            return result
        }
    }

    fun findLockEntryById(id: String) = lockEntrySet.find { it.id == id }
    fun addOrMerge(entry: LockEntry, mergeOp: (LockEntry?, LockEntry) -> LockEntry): LockEntry {
        synchronized(lockEntrySet) {
            val result = lockEntrySet.find { it.id == entry.id }?.let {
                lockEntrySet -= it
                mergeOp(it, entry)
            } ?: mergeOp(null, entry)
            lockEntrySet += result
            return result
        }
    }
}
