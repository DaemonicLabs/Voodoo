package voodoo.provider

import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import voodoo.curse.CurseClient
import voodoo.curse.CurseClient.findFile
import voodoo.curse.CurseClient.getAddon
import voodoo.curse.CurseClient.getAddonFile
import voodoo.curse.hash.Murmur2Lib
import voodoo.curse.hash.computeNormalizedArray
import voodoo.data.EntryReportData
import voodoo.data.curse.AddOnFileDependency
import voodoo.data.curse.CurseDependencyType
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.data.flat.Entry
import voodoo.data.lock.LockEntry
import voodoo.memoize
import voodoo.util.download
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.util.*
import kotlin.system.exitProcess

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */
object CurseProvider : ProviderBase("Curse Provider") {
    private val resolved = Collections.synchronizedList(mutableListOf<String>())

    override fun reset() {
        resolved.clear()
    }

    override suspend fun resolve(
        entry: Entry,
        mcVersion: String,
        addEntry: SendChannel<Pair<Entry, String>>
    ): LockEntry {
        entry as Entry.Curse
        val (projectID, fileID, path) = findFile(entry, mcVersion)

        synchronized(resolved) {
            logger.info("resolved: ${resolved.count()} unique entries")
            resolved += entry.id
        }
        // TODO: move into appropriate place or remove
        //  this is currently just used to validate that there is no entries getting resolved multiple times

        synchronized(resolved) {
            val count = resolved.count { entry.id == it }
            if (count > 1) {
                throw IllegalStateException("duplicate effort ${entry.id} entry counted: $count")
            }
        }

        resolveDependencies(projectID, fileID, entry, addEntry)

        entry.optional = isOptional(entry)
        logger.debug("entry.optiona = ${entry.optional}")

        if (!projectID.valid) {
            logger.error("invalid project id for $entry")
            throw IllegalStateException("invalid project id for $entry")
        }
        if (!fileID.valid) {
            logger.error("invalid file id for $entry")
            throw IllegalStateException("invalid file id for $entry")
        }

        entry.folder = path
        val lock = entry.lock { commonComponent ->
            LockEntry.Curse(
                common = commonComponent,
                projectID = projectID,
                fileID = fileID,
                useUrlTxt = entry.useUrlTxt,
                skipFingerprintCheck = entry.skipFingerprintCheck
            )
        }

        logger.debug("returning locked entry: $lock")
        return lock
    }

    override suspend fun generateName(entry: LockEntry): String {
        entry as LockEntry.Curse
        val addon = CurseClient.getAddon(entry.projectID)
        return addon?.name ?: entry.id
    }

    override suspend fun getAuthors(entry: LockEntry): List<String> {
        entry as LockEntry.Curse
        return CurseClient.getAuthors(entry.projectID)
    }

    override suspend fun getProjectPage(entry: LockEntry): String {
        entry as LockEntry.Curse
        return CurseClient.getProjectPage(entry.projectID)
    }

    override suspend fun getVersion(entry: LockEntry): String {
        entry as LockEntry.Curse
        val addonFile = getAddonFile(entry.projectID, entry.fileID)
        return addonFile?.fileName ?: ""
    }

    override suspend fun getLicense(entry: LockEntry): String {
        return getProjectPage(entry) + "/license"
    }

    override suspend fun getThumbnail(entry: LockEntry): String {
        entry as LockEntry.Curse
        val addon = CurseClient.getAddon(entry.projectID)
        return addon?.attachments?.firstOrNull { it.isDefault }?.thumbnailUrl ?: ""
    }

    override suspend fun getThumbnail(entry: Entry): String {
        entry as Entry.Curse
        val addon = CurseClient.getAddon(entry.projectID)
        return addon?.attachments?.firstOrNull { it.isDefault }?.thumbnailUrl ?: ""
    }

    private suspend fun resolveDependencies(
        addonId: ProjectID,
        fileId: FileID,
        entry: Entry.Curse,
        addEntry: SendChannel<Pair<Entry, String>>
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
                        val replacementId = entry.replaceDependencies[depAddon.id]
                        if (replacementId != null) {
                            if (replacementId != ProjectID.INVALID) {
                                logger.info("${entry.id} adding replaced dependency ${depAddon.id} ${depAddon.slug} -> $replacementId")
                                val replacementAddon = CurseClient.getAddon(replacementId) ?: run {
                                    logger.error("cannot resolve replacement dependency $replacementId")
                                    throw IllegalStateException("cannot resolve replacement dependency $replacementId")
                                }
                                dependsSet += replacementAddon.slug
                            } else {
                                logger.info("ignoring dependency ${depAddon.id} ${depAddon.slug}")
                            }
//                            dependsSet.forEach {
//                                entry.dependencies.putIfAbsent(it, depType)
//                            }
//                            continue
                        }

                        logger.info("${entry.id} adding dependency ${depAddon.id}  ${depAddon.slug}")
                        dependsSet += depAddon.slug
                    }
                    dependsSet.forEach {
                        entry.dependencies.putIfAbsent(it, depType)
                    }
                    logger.info("set dependency $curseDepType = $dependsSet")
                }

                val depEntry = Entry.Curse().apply {
                    id = depAddon.slug
                    name = entry.name
                    side = entry.side
                    transient = true
                    releaseTypes = entry.releaseTypes
                    projectID = depAddon.id
                }
                logger.debug("adding dependency: $depEntry")
                addEntry.send(depEntry to depAddon.categorySection.path)
                logger.debug("added dependency: $depEntry")
                logger.info("added $curseDepType dependency ${depAddon.name} of ${addon.name}")
            } else {
                continue
            }
        }
    }

    private fun isOptionalCall(entry: Entry): Boolean {
        ProviderBase.logger.info("test optional of ${entry.id}")
        return entry.transient || entry.optional
    }

    val isOptional = CurseProvider::isOptionalCall.memoize()

    override suspend fun download(
        stopwatch: Stopwatch,
        entry: LockEntry,
        targetFolder: File,
        cacheDir: File
    ): Pair<String?, File>? = stopwatch {
        entry as LockEntry.Curse
        val addonFile = getAddonFile(entry.projectID, entry.fileID)
        if (addonFile == null) {
            logger.error("cannot download ${entry.id} ${entry.projectID}:${entry.fileID}")
            exitProcess(3)
        }
        val targetFile = targetFolder.resolve(entry.fileName ?: addonFile.fileName)
        targetFile.download(
            url = addonFile.downloadUrl,
            cacheDir = cacheDir.resolve("CURSE").resolve(entry.projectID.toString()).resolve(entry.fileID.toString()),
            validator = { bytes, file ->
                val fileLenghtMatches = addonFile.fileLength == bytes.size.toLong()
                if(!fileLenghtMatches) {
                    logger.warn("[${entry.id} ${entry.projectID}:${addonFile.id}] file length do not match expected: ${addonFile.fileLength} actual: (${bytes.size})")
                }
                file.exists() && fileLenghtMatches && if (entry.skipFingerprintCheck) {
                    true
                } else {
//                    val normalized = computeNormalizedArray(file.readBytes())
                    val normalized = computeNormalizedArray(bytes)
                    val fileFingerprint = Murmur2Lib.hash32(normalized, 1)
                    if(addonFile.packageFingerprint.toInt() != fileFingerprint.toLong().toInt()) {
                        logger.error("[${entry.id} ${entry.projectID}:${addonFile.id}] file fingerprint does not match - expected: ${addonFile.packageFingerprint} actual: ($fileFingerprint) file: $file")
                        false
                    } else true
                }
            }
        )

        if(addonFile.fileLength != targetFile.length()) {
            error("[${entry.id} ${entry.projectID}:${addonFile.id}] file length do not match expected: ${addonFile.fileLength} actual: (${targetFile.length()})")
        }


        val normalized = computeNormalizedArray(targetFile.readBytes())
        val fileFingerprint = Murmur2Lib.hash32(normalized, 1)
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
}