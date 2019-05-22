package voodoo.provider

import MurmurHash2
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import voodoo.curse.CurseClient
import voodoo.curse.CurseClient.findFile
import voodoo.curse.CurseClient.getAddon
import voodoo.curse.CurseClient.getAddonFile
import voodoo.data.curse.AddOnFileDependency
import voodoo.data.curse.DependencyType
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.data.flat.Entry
import voodoo.data.lock.LockEntry
import voodoo.memoize
import voodoo.util.download
import java.io.File
import java.time.Instant
import java.util.Collections
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
        val (projectID, fileID, path) = findFile(entry, mcVersion, entry.curseMetaUrl)

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

        val lock = entry.lock {
            curseMetaUrl = entry.curseMetaUrl
            this.projectID = projectID
            this.fileID = fileID
            suggestedFolder = path
        }

        logger.debug("returning locked entry: $lock")
        return lock
    }

    override suspend fun generateName(entry: LockEntry): String {
        val addon = CurseClient.getAddon(entry.projectID, entry.curseMetaUrl)
        return addon?.name ?: entry.id
    }

    override suspend fun getAuthors(entry: LockEntry): List<String> {
        return CurseClient.getAuthors(entry.projectID, entry.curseMetaUrl)
    }

    override suspend fun getProjectPage(entry: LockEntry): String {
        return CurseClient.getProjectPage(entry.projectID, entry.curseMetaUrl)
    }

    override suspend fun getVersion(entry: LockEntry): String {
        val addonFile = getAddonFile(entry.projectID, entry.fileID, entry.curseMetaUrl)
        return addonFile?.fileName ?: ""
    }

    override suspend fun getLicense(entry: LockEntry): String {
        return getProjectPage(entry) + "/license"
    }

    override suspend fun getThumbnail(entry: LockEntry): String {
        val addon = CurseClient.getAddon(entry.projectID, entry.curseMetaUrl)
        return addon?.attachments?.firstOrNull { it.default }?.thumbnailUrl ?: ""
    }

    override suspend fun getThumbnail(entry: Entry): String {
        val addon = CurseClient.getAddonBySlug(entry.id, entry.curseMetaUrl)
        return addon?.attachments?.firstOrNull { it.default }?.thumbnailUrl ?: ""
    }

    private suspend fun resolveDependencies(
        addonId: ProjectID,
        fileId: FileID,
        entry: Entry,
        addEntry: SendChannel<Pair<Entry, String>>
    ) {
        val predefinedDependencies = entry.dependencies.flatMap { (depType, slugs) ->
            slugs.map { slug ->
                val id = CurseClient.deferredSlugIdMap.await()[slug] ?: throw IllegalStateException("cannot find id for slug: $slug")
                AddOnFileDependency(id, depType)
            }
        }
        val addon = getAddon(addonId, entry.curseMetaUrl)
            ?: throw IllegalStateException("addon $addonId could not be resolved, entry: $entry")
        val addonFile = getAddonFile(addonId, fileId, entry.curseMetaUrl)
            ?: throw IllegalStateException("addon file $addonId:$fileId could not be resolved, entry: $entry")
        val dependencies = predefinedDependencies + (addonFile.dependencies ?: listOf())

        logger.info("dependencies of ${entry.id} ${addonFile.dependencies}")
        logger.trace(entry.toString())

        for ((depAddonId, depType) in dependencies) {
            if (depType == DependencyType.RequiredDependency) {
                logger.info("resolve Dep $depAddonId")
                val depAddon = try {
                    getAddon(depAddonId, entry.curseMetaUrl, fail = true)
                } catch (e: Exception) {
                    null
                }

                if (depAddon == null) {
                    logger.error("broken dependency type: '$depType' id: '$depAddonId' of entry: '${entry.id}'")
                    continue
                }

//            val depends = entry.dependencies
                var dependsSet = entry.dependencies[depType]?.toSet() ?: setOf<String>()
                logger.info("get dependency $depType = $dependsSet + ${depAddon.slug}")
                if (!dependsSet.contains(depAddon.slug)) {
                    val replacementId = entry.replaceDependencies[depAddon.id]
                    if (replacementId != null) {
                        if (replacementId != ProjectID.INVALID) {
                            logger.info("${entry.id} adding replaced dependency ${depAddon.id} ${depAddon.slug} -> $replacementId")
                            val replacementAddon = CurseClient.getAddon(replacementId, entry.curseMetaUrl) ?: run {
                                logger.error("cannot resolve replacement dependency $replacementId")
                                throw IllegalStateException("cannot resolve replacement dependency $replacementId")
                            }
                            dependsSet += replacementAddon.slug
                        } else {
                            logger.info("ignoring dependency ${depAddon.id} ${depAddon.slug}")
                        }
                        entry.dependencies[depType] = dependsSet.toList()
                        continue
                    }

                    logger.info("${entry.id} adding dependency ${depAddon.id}  ${depAddon.slug}")
                    dependsSet += depAddon.slug
                }
                entry.dependencies[depType] = dependsSet.toList()
                logger.info("set dependency $depType = $dependsSet")

                val depEntry = Entry(provider = CurseProvider.id, id = depAddon.slug).apply {
                    name = entry.name
                    side = entry.side
                    transient = true
                    curseReleaseTypes = entry.curseReleaseTypes
                    curseProjectID = depAddon.id
                }
                logger.debug("adding dependency: $depEntry")
                addEntry.send(depEntry to depAddon.categorySection.path)
                logger.debug("added dependency: $depEntry")
                logger.info("added $depType dependency ${depAddon.name} of ${addon.name}")
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

    @ExperimentalUnsignedTypes
    override suspend fun download(entry: LockEntry, targetFolder: File, cacheDir: File): Pair<String, File> {
        val addonFile = getAddonFile(entry.projectID, entry.fileID, entry.curseMetaUrl)
        if (addonFile == null) {
            logger.error("cannot download ${entry.id} ${entry.projectID}:${entry.fileID}")
            exitProcess(3)
        }
        val targetFile = targetFolder.resolve(entry.fileName ?: addonFile.fileName)
        targetFile.download(
            addonFile.downloadURL,
            cacheDir.resolve("CURSE").resolve(entry.projectID.toString()).resolve(entry.fileID.toString()),
            canSkipDownload = { file ->
                file.exists()
//                addonFile.packageFingerprint.toUInt() != MurmurHash2.computeFileHash(file.path, true)
            }
        )
        val fileFingerprint = MurmurHash2.computeFileHash(targetFile.path, true)

        if (addonFile.packageFingerprint.toUInt() != fileFingerprint) {
            logger.error("[${entry.id} ${entry.projectID}:${addonFile.id}] file fingerprint does not match - expected: ${addonFile.packageFingerprint} actual: ($fileFingerprint)")
//            throw IllegalStateException("[${entry.id} ${entry.projectID}:${addonFile.id}] file fingerprints do not match expected: ${addonFile.packageFingerprint} actual: ($fileFingerprint)")
        }

        return Pair(addonFile.downloadURL, targetFile)
    }

    override suspend fun getReleaseDate(entry: LockEntry): Instant? {
        val addonFile = getAddonFile(entry.projectID, entry.fileID, entry.curseMetaUrl)
        return when (addonFile) {
            null -> return null
            else -> {
                addonFile.fileDate.toInstant()
            }
        }
    }

    override fun reportData(entry: LockEntry): MutableList<Triple<String, String, String>> {
        logger.debug("reporting for: $entry")
        val addon = runBlocking { getAddon(entry.projectID, entry.curseMetaUrl)!! }
        val addonFile = runBlocking { getAddonFile(entry.projectID, entry.fileID, entry.curseMetaUrl)!! }

        val data = super.reportData(entry)
        data += Triple("curse_release_type", "Release Type", "`${addonFile.releaseType}`")
        data += Triple(
            "curse_author",
            "Author",
            "`${addon.authors.sortedBy { it.name.toUpperCase() }.joinToString { it.name }}`"
        )
        return data
    }

    override fun validate(lockEntry: LockEntry): Boolean {
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