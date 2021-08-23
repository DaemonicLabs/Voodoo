package voodoo.provider

import Modloader
import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import voodoo.curse.CurseClient
import voodoo.data.EntryReportData
import voodoo.data.ModloaderPattern
import voodoo.data.curse.AddOnFileDependency
import voodoo.data.curse.CurseDependencyType
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.data.flat.FlatEntry
import voodoo.data.flat.FlatModPack
import voodoo.data.lock.LockEntry
import voodoo.labrinth.LabrinthClient
import voodoo.labrinth.ModId
import voodoo.labrinth.VersionFile
import voodoo.labrinth.VersionId
import voodoo.memoize
import voodoo.util.browserUserAgent
import voodoo.util.download
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import kotlin.system.exitProcess

object ModrinthProvider : ProviderBase("Modrinth Provider") {
    private val logger = KotlinLogging.logger {}

    override fun reset() {
//        resolved.clear()
    }

    override suspend fun resolve(
        entry: FlatEntry,
        modPack: FlatModPack,
        addEntry: suspend (FlatEntry) -> Unit
    ): FlatEntry {
        entry as FlatEntry.Modrinth

        //TODO: pick default version (latest),
        //   mark as replacable for merging code
        val (modId, versionId, path) = resolveVersion(entry, modPack.mcVersion, modPack.modloader)

        resolveDependencies(modId, versionId, entry, addEntry)

        entry.optional = isOptional(entry)
        logger.debug("entry.optiona = ${entry.optional}")

        if (!modId.valid) {
            logger.error("invalid mod id for $entry")
            throw IllegalStateException("invalid mod id for $entry")
        }
        if (!versionId.valid) {
            logger.error("invalid version id for $entry")
            throw IllegalStateException("invalid version id for $entry")
        }

        entry.folder = entry.folder ?: path

//        val resolved = entry.copy(
//            common = entry.common.copy(
//
//            ),
//            modrinth = entry.modrinth.copy(
//
//            )
//        )
//
//        logger.debug("returning locked entry: $resolved")
        return entry
    }

    private suspend fun resolveVersion(entry: FlatEntry.Modrinth, mcVersion: String, modloader: ModloaderPattern?): Triple<ModId, VersionId, String> {

    }

    private suspend fun getVersionFile(entry: LockEntry.Modrinth): VersionFile {
        val version = LabrinthClient.version(entry.versionId)
        val regex = entry.fileNameRegex.toRegex()
        return version.files.firstOrNull { it.filename.matches(regex) } ?: error("")
    }

    override fun lock(entry: FlatEntry, modPack: FlatModPack): LockEntry {
        entry as FlatEntry.Modrinth
        val common = entry.lockCommon()
        return LockEntry.Modrinth(
            id = common.id,
            path = common.path,
            name = common.name,
            fileName = common.fileName,
            side = common.side,
            description = common.description,
            optionalData = common.optionalData,
            dependencies = common.dependencies,
            slug = entry.slug,
            modId = entry.modId,
            versionId = entry.versionId,
            fileNameRegex = entry.fileNameRegex,
            useOriginalUrl = entry.useOriginalUrl,
            skipFingerprintCheck = entry.skipFingerprintCheck
        )
    }

    override suspend fun generateName(entry: LockEntry): String {
        entry as LockEntry.Modrinth
        return LabrinthClient.getMod(entry.slug).title
    }

    override suspend fun getAuthors(entry: LockEntry): List<String> {
        entry as LockEntry.Modrinth
        // TODO: fix
        return listOf(
            LabrinthClient.getMod(entry.slug).team
        )
    }

    override suspend fun getProjectPage(entry: LockEntry): String {
        entry as LockEntry.Modrinth
        return "https://modrinth.com/mod/${entry.slug}"
    }

    override suspend fun getVersion(entry: LockEntry): String {
        entry as LockEntry.Modrinth
        val file = getVersionFile(entry)
        return file.filename
    }

    override suspend fun getLicense(entry: LockEntry): String {
        entry as LockEntry.Modrinth
        return LabrinthClient.getMod(entry.slug).license.name
    }

    override suspend fun getThumbnail(entry: LockEntry): String {
        entry as LockEntry.Modrinth
        return LabrinthClient.getMod(entry.slug).icon_url ?: ""
    }

    override suspend fun getThumbnail(entry: FlatEntry): String {
        entry as FlatEntry.Modrinth
        return LabrinthClient.getMod(entry.slug).icon_url ?: ""
    }

    private suspend fun resolveDependencies(
        addonId: ProjectID,
        fileId: FileID,
        entry: FlatEntry.Curse,
        addEntry: suspend (FlatEntry) -> Unit
    ) {
        val predefinedDependencies = entry.dependencies.map { (slug, depType) ->
            val id = CurseClient.getProjectIdBySlug(slug)
                ?: throw IllegalStateException("cannot find id for slug: $slug")
            AddOnFileDependency(id, CurseDependencyType.values().first { depType == it.depType })
        }
        val addon = getAddon(addonId)
            ?: throw IllegalStateException("addon $addonId could not be resolved, entry: $entry")
        val addonFile = getAddonFile(addonId, fileId)
            ?: throw IllegalStateException("addon file $addonId:$fileId could not be resolved, entry: $entry")
        val dependencies = predefinedDependencies + (addonFile.dependencies ?: listOf())

        logger.info("dependencies of ${entry.id} ${addonFile.dependencies}")
        logger.trace(entry.toString())

        for ((depAddonId, curseDepType) in dependencies) {
            if (curseDepType == CurseDependencyType.RequiredDependency) {
                logger.info("resolve Dep $depAddonId")
                val depAddon = try {
                    getAddon(depAddonId, fail = true)
                } catch (e: Exception) {
                    null
                }

                if (depAddon == null) {
                    logger.error("broken dependency type: '$curseDepType' id: '$depAddonId' of entry: '${entry.id}'")
                    continue
                }

//            val depends = entry.dependencies
                val depType = curseDepType.depType
                if (depType != null) {
                    var dependsSet = entry.dependencies.filterValues { it == depType }.keys
                    logger.info("get dependency $curseDepType = $dependsSet + ${depAddon.slug}")
                    if (!dependsSet.contains(depAddon.slug)) {
                        // TODO: use string keys in replaceDependencies
//                        val replacementId = entry.replaceDependencies[depAddon.id]
//                        if (replacementId != null) {
//                            if (replacementId != ProjectID.INVALID) {
//                                logger.info("${entry.id} adding replaced dependency ${depAddon.id} ${depAddon.slug} -> $replacementId")
//                                val replacementAddon = CurseClient.getAddon(replacementId) ?: run {
//                                    logger.error("cannot resolve replacement dependency $replacementId")
//                                    throw IllegalStateException("cannot resolve replacement dependency $replacementId")
//                                }
//                                dependsSet += replacementAddon.slug
//                            } else {
//                                logger.info("ignoring dependency ${depAddon.id} ${depAddon.slug}")
//                            }
//                        }

                        logger.info("${entry.id} adding dependency ${depAddon.id}  ${depAddon.slug}")
                        dependsSet += depAddon.slug
                    }
                    dependsSet.forEach {
                        entry.dependencies.putIfAbsent(it, depType)
                    }
                    logger.info("set dependency $curseDepType = $dependsSet")
                }

                val depEntry = FlatEntry.Curse().apply {
                    id = depAddon.slug
                    name = entry.name
                    side = entry.side
                    transient = true
                    releaseTypes = entry.releaseTypes
                    validMcVersions = entry.validMcVersions
                    projectID = depAddon.id
                    folder = depAddon.categorySection.path
                    useOriginalUrl = entry.useOriginalUrl
                }
                logger.debug("adding dependency: $depEntry")
                addEntry(depEntry)
                logger.debug("added dependency: $depEntry")
                logger.info("added $curseDepType dependency ${depAddon.name} of ${addon.name}")
            } else {
                continue
            }
        }
    }

    private fun isOptionalCall(entry: FlatEntry): Boolean {
        logger.info("test optional of ${entry.id}")
        return entry.transient || entry.optional
    }

    val isOptional = ModrinthProvider::isOptionalCall.memoize()

    override suspend fun download(
        stopwatch: Stopwatch,
        entry: LockEntry,
        targetFolder: File,
        cacheDir: File
    ): Pair<String?, File>? = stopwatch {
        entry as LockEntry.Modrinth
        val addonFile = getAddonFile(entry.projectID, entry.fileID)
        if (addonFile == null) {
            logger.error("cannot download ${entry.id} ${entry.projectID}:${entry.fileID}")
            exitProcess(3)
        }
        val targetFile = targetFolder.resolve(entry.fileName ?: addonFile.fileName)
        targetFile.download(
            url = addonFile.downloadUrl,
            cacheDir = cacheDir.resolve("CURSE").resolve(entry.projectID.toString()).resolve(entry.fileID.toString()),
            validator = { file ->
                val bytes = file.readBytes()
                val fileLenghtMatches = addonFile.fileLength == bytes.size.toLong()
                if(!fileLenghtMatches) {
                    logger.warn("[${entry.id} ${entry.projectID}:${addonFile.id}] file length do not match expected: ${addonFile.fileLength} actual: (${bytes.size})")
                }
                file.exists() && fileLenghtMatches && if (entry.skipFingerprintCheck) {
                    true
                } else {
                    val normalized = computeNormalizedArray(bytes)
                    val fileFingerprint = Murmur2.hash32(normalized, seed = 1)
                    val fileFingerprintOld = Murmur2Lib.hash32(normalized)
                    if(fileFingerprint != fileFingerprintOld) {
                        logger.error { "file fingerprint differs: $fileFingerprint != $fileFingerprintOld" }
                    }
                    if(addonFile.packageFingerprint.toInt() != fileFingerprint.toLong().toInt()) {
                        logger.error("[${entry.id} ${entry.projectID}:${addonFile.id}] file fingerprint does not match - expected: ${addonFile.packageFingerprint} actual: ($fileFingerprint) file: $file")
                        false
                    } else true
                }
            },
            retries = 5,
            useragent = browserUserAgent,
        )

        if(addonFile.fileLength != targetFile.length()) {
            error("[${entry.id} ${entry.projectID}:${addonFile.id}] file length do not match expected: ${addonFile.fileLength} actual: (${targetFile.length()})")
        }
        val normalized = computeNormalizedArray(targetFile.readBytes())
        val fileFingerprint = Murmur2.hash32(normalized, seed = 1)
        val fileFingerprintOld = Murmur2Lib.hash32(normalized)

        if(fileFingerprint != fileFingerprintOld) {
            logger.error { "file fingerprint differs: $fileFingerprint != $fileFingerprintOld" }
        }
//        val fileFingerprint = MurmurHash2.computeFileHash(targetFile.path, true)

        if (addonFile.packageFingerprint.toInt() != fileFingerprint.toLong().toInt()) {
            logger.error("[${entry.id} ${entry.projectID}:${addonFile.id}] file fingerprint does not match - expected: ${addonFile.packageFingerprint} actual: ($fileFingerprint)")
            logger.error("curseforge murmur2 fingerprints are unreliable")
            if(!entry.skipFingerprintCheck) {
                error("[${entry.id} ${entry.projectID}:${addonFile.id}] file fingerprints do not match expected: ${addonFile.packageFingerprint} actual: ($fileFingerprint)")
            }
        } else {
            logger.debug { "[${entry.id} ${entry.projectID}:${addonFile.id}] file fingerprint matches: ${addonFile.packageFingerprint}" }
        }

        return@stopwatch addonFile.downloadUrl to targetFile
    }

    override suspend fun getReleaseDate(entry: LockEntry): Instant? {
        entry as LockEntry.Curse
        val addonFile = getAddonFile(entry.projectID, entry.fileID)
        return when (addonFile) {
            null -> return null
            else -> {
                addonFile.fileDate.toInstant(ZoneOffset.UTC)
            }
        }
    }

    override fun reportData(entry: LockEntry): MutableMap<EntryReportData, String> {
        entry as LockEntry.Curse
        logger.debug("reporting for: $entry")
        val addon = runBlocking { getAddon(entry.projectID)!! }
        val addonFile = runBlocking { getAddonFile(entry.projectID, entry.fileID)!! }
        return super.reportData(entry).also { data ->
            data[EntryReportData.FILE_NAME] = entry.fileName ?: addonFile.fileName
            data[EntryReportData.DIRECT_URL] = addonFile.downloadUrl
            data[EntryReportData.CURSE_RELEASE_TYPE] = "${addonFile.releaseType}"
            // TODO: support lists ?
            data[EntryReportData.CURSE_AUTHORS] = addon.authors.sortedBy { it.name.toUpperCase() }.joinToString { it.name }
        }
    }

    override fun generateReportTableOverrides(entry: LockEntry): Map<String, Any?> {
        entry as LockEntry.Curse
        logger.debug("reporting for: $entry")
        val addon = runBlocking { getAddon(entry.projectID)!! }
        val addonFile = runBlocking { getAddonFile(entry.projectID, entry.fileID)!! }

        return mapOf(
            "File Name" to (entry.fileName ?: addonFile.fileName),
            "Direct URL" to addonFile.downloadUrl,
            "Release Type" to "${addonFile.releaseType}",
            "Mod Authors" to addon.authors.sortedBy { it.name.toUpperCase() }
        )
    }

    override fun validate(lockEntry: LockEntry): Boolean {
        if (lockEntry !is LockEntry.Curse) {
            logger.warn("invalid type for Curse $lockEntry")
            return false
        }
        if (!super.validate(lockEntry)) {
            return false
        }
        if (!lockEntry.projectID.valid) {
            logger.warn("invalid project id")
            return false
        }
        if (!lockEntry.fileID.valid) {
            logger.warn("invalid file id")
            return false
        }
        return true
    }

    suspend fun findFile(
        entry: FlatEntry.Curse,
        mcVersion: String,
        modloader: ModloaderPattern?
    ): Triple<ProjectID, FileID, String> {
        val mcVersions = listOf(mcVersion) + entry.validMcVersions
        val slug = entry.id // TODO: maybe make into separate property
        val version = entry.version
        val releaseTypes = entry.releaseTypes
        var addonId = entry.projectID
        val fileNameRegex = entry.fileNameRegex

        if(modloader is ModloaderPattern.Forge) {
            entry.invalidMcVersions += "Fabric"
        }
        if(modloader is ModloaderPattern.Fabric) {
            entry.invalidMcVersions += "Forge"
        }

        val addon = if (!addonId.valid) {
//            slug.takeUnless { it.isBlank() }
//                ?.let { getAddonBySlug(it) }
            CurseClient.logger.error { "addon id $addonId is invalid" }
            throw java.lang.IllegalStateException("$addonId is invalid")
        } else {
            CurseClient.getAddon(addonId)
        }

        if (addon == null) {
            CurseClient.logger.error("no addon matching the parameters found for '$entry'")
            kotlin.system.exitProcess(-1)
//            return Triple(ProjectID.INVALID, FileID.INVALID, "")
        }

        addonId = addon.id

        if (entry.fileID.valid) {
            val file = CurseClient.getAddonFile(addonId, entry.fileID)!!
            return Triple(addonId, file.id, addon.categorySection.path)
        }

        val re = Regex(fileNameRegex)

        var files = CurseClient.getAllFilesForAddon(addonId).sortedWith(compareByDescending { it.fileDate })

        var oldFiles = files

        if (version != null && version.isNotBlank()) {
            files = files.filter { f ->
                (f.fileName.contains(version.toRegex()) || f.fileName == version)
            }
            if (files.isEmpty()) {
                CurseClient.logger.error("filtered files did not match version $version $oldFiles")
            }
            oldFiles = files
        }

        if (files.isNotEmpty()) {
            files = files.filter { f ->
                mcVersions.any { v -> f.gameVersion.contains(v) }
            }

            if (files.isEmpty()) {
                CurseClient.logger.error("validMcVersions: $mcVersion + ${entry.validMcVersions}")
                CurseClient.logger.error("filtered files did not match mcVersions: $mcVersions + ${entry.validMcVersions} $oldFiles")
            }
            oldFiles = files
        }

        if (files.isNotEmpty()) {
            files = files.filterNot { f ->
                entry.invalidMcVersions.any { v -> f.gameVersion.contains(v) }
            }

            if (files.isEmpty()) {
                CurseClient.logger.error("invalidMcVersions: ${entry.invalidMcVersions}")
                CurseClient.logger.error("filtered files did match invalidMcVersions: ${entry.invalidMcVersions} $oldFiles")
            }
            oldFiles = files
        }

        if (files.isNotEmpty()) {
            files = files.filter { f ->
                releaseTypes.contains(f.releaseType)
            }

            if (files.isEmpty()) {
                CurseClient.logger.error("filtered files did not match releaseType $releaseTypes $oldFiles")
            }
            oldFiles = files
        }

        if (files.isNotEmpty()) {
            files = files.filter { f ->
                re.matches(f.fileName)
            }
            if (files.isEmpty()) {
                CurseClient.logger.error("filtered files did not match regex {}", oldFiles)
            }
        }

        val file = files.asSequence().sortedWith(compareByDescending { it.fileDate }).firstOrNull()
        if (file == null) {
            val filesUrl = "${CurseClient.ADDON_API}/addon/$addonId/files"
            CurseClient.logger.error(
                "no matching version found for ${addon.name} addon_url: ${addon.websiteUrl} " +
                        "files: $filesUrl mc version: $mcVersions version: $version"
            )
            CurseClient.logger.error("no file matching the parameters found for ${addon.name}")
            CurseClient.logger.error("filtered by")
            CurseClient.logger.error("mcVersions: $mcVersions")
            CurseClient.logger.error("releaseTypes: $releaseTypes")
            CurseClient.logger.error("filename: $re")
            kotlin.system.exitProcess(-1)
//            return Triple(addonId, FileID.INVALID, "")
        }
        return Triple(addonId, file.id, addon.categorySection.path)
    }
}