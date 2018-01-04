package moe.nikky.builder.provider

import aballano.kotlinmemoization.memoize
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import khttp.get
import moe.nikky.builder.*
import java.io.File

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */
class CurseProviderThingy : ProviderThingy() {
    companion object {
        val mapper = jacksonObjectMapper() // Enable YAML parsing
                .registerModule(KotlinModule())!! // Enable Kotlin support
        private val META_URL = "https://cursemeta.nikky.moe"
        val data: List<Addon> = getAddonData()

        private fun getAddonData(): List<Addon> {
//            val url = "$META_URL/api/addon/?mods=1&texturepacks=1&worlds=1&property=id,name,summary,websiteURL,packageType,categorySection"
            val url = "$META_URL/api/addon/?mods=1&texturepacks=1&worlds=1"

            println(url)
            val r = get(url)
            if (r.statusCode == 200) {
                return mapper.readValue(r.text)
            }
            throw Exception("failed getting cursemeta data")
        }
    }

    override val name = "Curse Provider"
    init {
        register("prepareDependencies",
                { (it.id <= 0 || it.fileId <= 0) && it.name.isNotBlank() },
                { e, m ->
                    val (addonId, fileId, fileName) = findFile(e, m)

                    e.id = addonId
                    e.fileId = fileId

                    if (e.fileName.isBlank()) {
                        e.fileName = fileName
                    }
                }
        )
        register("resolveDependencies",
                { it.id > 0 && it.fileId > 0 },
                ::resolveDependencies
        )
        register("setName",
                { it.id > 0 && it.name.isBlank() },
                { e, _ ->
                    e.name = getAddon(e.id)!!.name
                }
        )
        register("setDescription",
                { it.id > 0 && it.description.isBlank() },
                { e, _ ->
                    e.description = getAddon(e.id)!!.summary
                }
        )
        register("setWebsiteUrl",
                { it.id > 0 && it.websiteUrl.isBlank() },
                { e, _ ->
                    e.url = getAddon(e.id)!!.webSiteURL
                }
        )
        register("setUrl",
                { it.id > 0 && it.fileId > 0 && it.url.isBlank() },
                { e, _ ->
                    e.url = getAddonFile(e.id, e.fileId)!!.downloadURL
                }
        )
        register("setFileName",
                { it.id > 0 && it.fileId > 0 && it.fileName.isBlank() },
                { e, _ ->
                    e.fileName = getAddonFile(e.id, e.fileId)!!.fileNameOnDisk
                }
        )
        register("setPackageType",
                { it.id > 0 && it.packageType == PackageType.none },
                { e, _ ->
                    e.packageType = getAddon(e.id)!!.packageType
                }
        )
        register("setTargetPath",
                { it.id > 0 && it.targetPath.isBlank() },
                { e, _ ->
                    e.targetPath = getAddon(e.id)!!.categorySection.path
                }
        )
        register("cacheRelpath",
                { it.cacheRelpath.isBlank() },
                { e, _ ->
                    e.cacheRelpath = File(e.provider.toString()).resolve(e.id.toString()).resolve(e.fileId.toString()).path
                }
        )
        register("prepareDownload",
                {
                    with(it) {
                        listOf(url, name, fileName, cachePath).all { it.isNotBlank() }
                    }
                },
                { e, _ ->
                    e.provider = Provider.DIRECT
                }
        )
    }

    fun resolveDependencies(entry: Entry, modpack: Modpack) {
        val addonId = entry.id
        val fileId = entry.fileId
        val addon = getAddon(addonId)!!
        val addonFile = getAddonFile(addonId, fileId)!!

        for ((depAddonId, depType) in addonFile.dependencies) {

            val depAddon = getAddon(depAddonId) ?: continue

//            val depends = entry.dependencies
            var dependsList = entry.dependencies.getOrDefault(depType, emptyList())
            dependsList += depAddon.name
            entry.dependencies[depType] = dependsList

            // find duplicate entry
            var depEntry = modpack.entries.firstOrNull { e ->
                e.provider == Provider.CURSE &&
                        (e.id == depAddon.id || e.name == depAddon.name)
            }
            if (depEntry == null) {
                if (depType == DependencyType.required || (modpack.doOptionals && depType == DependencyType.optional)) {
                    //TODO: WORKAROUND, add this as a normal entry to be processed later
//                        val (depAddonId, depFileId, fileName) = findFile(Entry(
//                                provider = Provider.CURSE,
//                                id = addOnId,
//                                curseFileNameRegex = entry.curseFileNameRegex
//                        ), modpack)
//                        if (depAddonId < 0 || depFileId < 0)
//                            throw Exception("dependency resolution error for $depType dependency ${depAddon.name} " +
//                                    "${depAddon.id} of ${addon.name} ${addon.id}")
//                        // depAddon = getAddon(depAddonId)
                    depEntry = Entry(
                            provider = Provider.CURSE,
                            id = depAddon.id,
                            name = depAddon.name
                    )
                    println(depEntry)
                    modpack.entries += depEntry
                    println("added $depType dependency ${depAddon.name} of ${addon.name}")
                }
            } else {
                val otherSide = depEntry.side
                val side = Side.values().find { s -> s.flag == otherSide.flag or entry.side.flag }
                if (side == null) throw Exception("invalid side")

                var provideList = depEntry.provides[depType] ?: emptyList()
                provideList += addon.name
                depEntry.provides[depType] = provideList
            }
        }
    }

    private fun findFile(entry: Entry, modpack: Modpack): Triple<Int, Int, String> {
        val mcVersions = listOf(modpack.mcVersion) + modpack.validMcVersions
        val name = entry.name
        val version = entry.version
        var releaseTypes = entry.releaseTypes
//        if(releaseTypes.isEmpty()) {
//            releaseTypes = setOf(ReleaseType.RELEASE, ReleaseType.BETA) //TODO: is this not already set because i enforce defaults ?
//        }
        var addonId = entry.id
        var fileId = entry.fileId
        val fileNameRegex = entry.curseFileNameRegex

//        data.forEach { addon -> println("${addon.id} ${addon.name}")}

        val addon = data.find { addon ->
            (name.isNotBlank() && name.equals(addon.name, true))
                    || (addonId > 0 && addonId == addon.id)
        } ?: return Triple(-1, -1, "")

        addonId = addon.id

        val re = Regex(fileNameRegex)

        if (fileId > 0) {
            val file = getAddonFile(addonId, fileId)
            if (file != null)
                return Triple(addonId, file.id, file.fileNameOnDisk)
        }

        var files = getAllAddonFiles(addonId)
//        println("mc version: $mcVersion")
//        files.forEach{f -> println("${f.fileName} ${f.fileNameOnDisk} ${f.gameVersion}")}

        files = files.filter { f ->
            ((version.isNotBlank()
                    && f.fileName.contains(version, true) || version.isBlank()) &&
                    mcVersions.any { v -> f.gameVersion.contains(v) } &&
                    releaseTypes.contains(f.releaseType) &&
                    re.matches(f.fileName))
        }.sortedWith(compareByDescending { it.fileDate })

//        println("filtered")
//        files.forEach{f -> println("filtered ${f.fileName} ${f.fileNameOnDisk} ${f.gameVersion}")}

        val file = files.firstOrNull()
        if (file != null)
            return Triple(addonId, file.id, file.fileNameOnDisk)
        println(addon) //TODO: turn into error dump to disk and just print filepath
        println("no matching version found for ${addon.name} addon_url: ${addon.webSiteURL} " +
                "mc version: $mcVersions version: $version")
//        // TEST
//        for (addon1 in data.sortedBy { a -> Math.round(Math.random()-0.5) }) {
//            getAllAddonFiles(addon1.id)
//        }

        println("no file matching the parameters found for ${addon.name}")
        return Triple(addonId, -1, "")
    }

    private fun getAddonFileCall(addonId: Int, fileId: Int): AddonFile? {
        val url = "${META_URL}/api/addon/$addonId/files/$fileId"

        println(url)
        val r = get(url)
        if (r.statusCode == 200) {
            return mapper.readValue(r.text)
        }
        return null
    }

    val getAddonFile = ::getAddonFileCall.memoize()

    private fun getAllAddonFilesCall(addonId: Int): List<AddonFile> {
        val url = "${META_URL}/api/addon/$addonId/files"

        println(url)
        val r = get(url)
        if (r.statusCode == 200) {
            return mapper.readValue(r.text)
        }
        throw Exception("failed getting cursemeta data")
    }

    val getAllAddonFiles = ::getAllAddonFilesCall.memoize()

    private fun getAddonCall(addonId: Int): Addon? {
        val url = "${META_URL}/api/addon/$addonId"

        println(url)
        val r = get(url)
        if (r.statusCode == 200) {
            return mapper.readValue(r.text)
        }
        return null
    }

    val getAddon = ::getAddonCall.memoize()



//    override fun validate(): Boolean {
//        return entry.fileId > 0 && entry.id > 0
//    }

//    override fun prepareDependencies(modpack: Modpack) {
////        val (addonId, fileId, fileName) = findFile(entry, modpack)
////        entry.id = addonId
////        entry.fileId = fileId
////        if (entry.fileName.isBlank()) {
////            entry.fileName = fileName
////        }
//    }
//
//
//    override fun resolveDependencies(modpack: Modpack) {
//        val addonId = entry.id
//        val fileId = entry.fileId
//        val addon = getAddon(addonId)!!
//        val addonFile = getAddonFile(addonId, fileId)!!
//
//        for ((addOnId, depType) in addonFile.dependencies) {
//
//            val depAddon = getAddon(addOnId) ?: continue
//
////            val depends = entry.dependencies
//            var dependsList = entry.dependencies.getOrDefault(depType, emptyList())
//            dependsList += depAddon.name
//            entry.dependencies[depType] = dependsList
//
//            // find duplicate entry
//            var depEntry = modpack.entries.filter { e ->
//                e.provider == Provider.CURSE &&
//                        (e.id == depAddon.id || e.name == depAddon.name)
//            }.firstOrNull()
//            if (depEntry == null) {
//                if (depType == DependencyType.required || (modpack.doOptionals && depType == DependencyType.optional)) {
//                    //TODO: WORKAROUND, add this as a normal entry to be processed later
//                    val (depAddonId, depFileId, fileName) = findFile(Entry(
//                            provider = Provider.CURSE,
//                            id = addOnId,
//                            curseFileNameRegex = entry.curseFileNameRegex
//                    ), modpack)
//                    if (depAddonId < 0 || depFileId < 0)
//                        throw Exception("dependency resolution error for $depType dependency ${depAddon.name} " +
//                                "${depAddon.id} of ${addon.name} ${addon.id}")
//                    // depAddon = getAddon(depAddonId)
//                    depEntry = Entry(
//                            provider = Provider.CURSE,
//                            id = depAddonId,
//                            fileId = depFileId,
//                            name = depAddon.name
//                    )
//                    modpack.entries += depEntry
//                    println("added $depType dependency $fileName of ${addon.name}")
//                }
//            } else {
//                val otherSide = depEntry.side
//                val side = Side.values().find { s -> s.flag == otherSide.flag or entry.side.flag }
//                if (side == null) throw Exception("invalid side")
//
//                var provideList = depEntry.provides[depType] ?: emptyList()
//                provideList += addon.name
//                depEntry.provides[depType] = provideList
//            }
//        }
//    }
//
//    override fun fillInformation() {
//        println(entry)
//        val addon = getAddon(entry.id) ?: throw Exception("id ${entry.id} is invalid")
//        val addonFile = getAddonFile(entry.id, entry.fileId) ?: throw Exception("id:fileId ${entry.id} : ${entry.fileId} is invalid")
//        if (entry.name.isBlank()) {
//            entry.name = addon.name
//        }
//        if (entry.description.isBlank()) {
//            entry.description = addon.summary
//        }
//        if (entry.fileName.isBlank()) {
//            entry.fileName = addonFile.fileNameOnDisk
//        }
//        if (entry.url.isBlank()) {
//            entry.url = addonFile.downloadURL
//        }
//        if (entry.websiteUrl.isBlank()) {
//            entry.websiteUrl = addon.webSiteURL
//        }
//        if (entry.packageType == PackageType.none) {
//            entry.packageType = addon.packageType
//        }
//        if (entry.path.isBlank()) {
//            entry.path = addon.categorySection.path
//        }
//
////        for ((depType, list) in entry.provides) {
////            var newList = emptyList<String>()
////            for (provided in list) {
////
////            }
////        }
////        provides = entry.get('provides', {})
////        for release_type in provides.keys():
////            provide_list = provides[release_type]
////            new_list = []
////            for provider in provide_list:
////                if isinstance(provider, int):
////                    provide_addon = self.get_add_on(provider)
////                    new_list.append(provide_addon['name'])
////            provides[str(release_type)] = new_list
////        entry['provides'] = provides
//
//        super.fillInformation()
//    }
//
//    override fun prepareDownload(cacheBase: File) {
//        entry.provider = Provider.DIRECT
//        if (entry.cacheBase.isBlank()) {
//            entry.cacheBase = cacheBase.absolutePath
//        }
//        if (entry.cachePath.isBlank()) {
//            entry.cachePath = "${entry.cacheBase}/${entry.id}/${entry.fileId}"
//        }
//
//    }
//
//    override fun download(outputPath: File) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//
//    private fun findFile(entry: Entry, modpack: Modpack): Triple<Int, Int, String> {
//        val mcVersion = modpack.mcVersion
//        val name = entry.name
//        val version = entry.version
//        var releaseTypes = entry.releaseTypes
////        if(releaseTypes.isEmpty()) {
////            releaseTypes = setOf(ReleaseType.RELEASE, ReleaseType.BETA) //TODO: is this not already set because i enforce defaults ?
////        }
//        var addonId = entry.id
//        var fileId = entry.fileId
//        val fileNameRegex = entry.curseFileNameRegex
//
////        data.forEach { addon -> println("${addon.id} ${addon.name}")}
//
//        val addon = data.find { addon ->
//            (name.isNotBlank() && name.equals(addon.name, true))
//                    || (addonId > 0 && addonId == addon.id)
//        } ?: return Triple(-1, -1, "")
//
//        addonId = addon.id
//
//        val re = Regex(fileNameRegex)
//
//        if (fileId > 0) {
//            val file = getAddonFile(addonId, fileId)
//            if (file != null)
//                return Triple(addonId, file.id, file.fileNameOnDisk)
//        }
//
//        var files = getAllAddonFiles(addonId)
////        println("mc version: $mcVersion")
////        files.forEach{f -> println("${f.fileName} ${f.fileNameOnDisk} ${f.gameVersion}")}
//
//        files = files.filter { f ->
//            ((version.isNotBlank()
//                    && f.fileName.contains(version, true) || version.isBlank()) &&
//                    mcVersion.any { v -> f.gameVersion.contains(v) } &&
//                    releaseTypes.contains(f.releaseType) &&
//                    re.matches(f.fileName))
//        }.sortedWith(compareByDescending { it.fileDate })
//
////        println("filtered")
////        files.forEach{f -> println("filtered ${f.fileName} ${f.fileNameOnDisk} ${f.gameVersion}")}
//
//        val file = files.firstOrNull()
//        if (file != null)
//            return Triple(addonId, file.id, file.fileNameOnDisk)
//        println(addon) //TODO: turn into error dump to disk and just print filepath
//        println("no matching version found for ${addon.name} addon_url: ${addon.webSiteURL} " +
//                "mc version: $mcVersion version: $version")
////        // TEST
////        for (addon1 in data.sortedBy { a -> Math.round(Math.random()-0.5) }) {
////            getAllAddonFiles(addon1.id)
////        }
//
//        println("no file matching the parameters found for ${addon.name}")
//        return Triple(addonId, -1, "")
//    }
//
//
//    fun doCurseThingy() {
//        println("doCurseThingy not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
}


enum class ReleaseType {
    release, beta, alpha
}

data class Addon(
        val attachments: List<Attachment> = emptyList(),
        val authors: List<Author>,
        val categories: List<Category>,
        val categorySection: CategorySection,
        val commentCount: Int,
        val defaultFileId: Int,
        val downloadCount: Float,
        val gameId: Int,
        val gamePopularityRank: Int,
        val gameVersionLatestFiles: List<GameVersionlatestFile>,
        val iconId: Int,
        val id: Int,
        val installCount: Int,
        val isFeatured: Int,
        val latestFiles: List<AddonFile>,
        val likes: Int,
        val name: String,
        val packageType: PackageType,
        val popularityScore: Float,
        val primaryAuthorName: String?,
        val primaryCategoryAvatarUrl: String?,
        val primaryCategoryId: Int?,
        val primaryCategoryName: String?,
        val rating: Int,
        val stage: CurseStage,
        val status: FileStatus,
        val summary: String,
        val webSiteURL: String
)

data class AddonFile(
        val id: Int,
        val alternateFileId: Int,
        val dependencies: List<CurseDependency>,
        val downloadURL: String,
        val fileDate: String,
        val fileName: String,
        val fileNameOnDisk: String,
        val fileStatus: FileStatus,
        val gameVersion: List<String>,
        val isAlternate: Boolean,
        val isAvailable: Boolean,
        val packageFingerprint: Long,
        var releaseType: ReleaseType,
        var modules: List<CurseModule>
)

enum class FileStatus {
    normal, semiNormal, deleted
}

enum class DependencyType {
    required, optional, embedded
}

enum class PackageType {
    none, mod, folder, file, singleFile
}

enum class CurseStage {
    release, alpha, beta, inactive, deleted
}

data class GameVersionlatestFile(
        val fileType: ReleaseType,
        val gameVesion: String,
        val projectFileID: Int,
        val projectFileName: String
)

data class CurseModule(
        val fingerprint: Long,
        val foldername: String
)

data class CurseDependency(
        val addOnId: Int,
        val type: DependencyType
)

data class CategorySection(
        val gameID: Int,
        val id: Int,
        val extraIncludePattern: String = "",
        val initialInclusionPattern: String,
        val name: String,
        val packageType: PackageType,
        val path: String
)

data class Category(
        val id: Int,
        val name: String,
        val url: String
)

data class Author(
        val name: String,
        val url: String
)

data class Attachment(
        val description: String,
        val isDefault: Boolean,
        val thumbnailUrl: String,
        val title: String,
        val url: String
)