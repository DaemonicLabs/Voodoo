package voodoo.provider

import mu.KLogging
import voodoo.curse.CurseClient
import voodoo.curse.CurseClient.findFile
import voodoo.curse.CurseClient.getAddon
import voodoo.curse.CurseClient.getAddonFile
import voodoo.data.curse.DependencyType
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.memoize
import voodoo.util.download
import java.io.File
import java.time.Instant
import kotlin.system.exitProcess

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */
object CurseProviderThing : ProviderBase, KLogging() {
    override val name = "Curse Provider"
    val resolved = mutableListOf<String>()

    override suspend fun resolve(entry: Entry, modpack: ModPack, addEntry: (Entry, String) -> Unit): LockEntry {
        val (projectID, fileID, path) = findFile(entry, modpack.mcVersion, entry.curseMetaUrl)

        logger.info("resolved: $resolved")
        resolved += entry.id

        //TODO: move into appropriate place or remove
        // this is currently just used to validate that there is no entries getting resolved multiple times
        val count = resolved.count { entry.id == it }
        if (count > 1) {
            throw Exception("duplicate effort ${entry.id} entry counted: $count")
        }

        resolveDependencies(projectID, fileID, entry, addEntry)

        entry.optional = isOptional(entry, modpack)

        return LockEntry(
                provider = entry.provider,
                id = entry.id,
                name = entry.name,
                curseMetaUrl = entry.curseMetaUrl,
                //rootFolder = path, //maybe use entry.rootFolder only if its non-default
                useUrlTxt = entry.useUrlTxt,
                fileName = entry.fileName,
                side = entry.side,
                projectID = projectID,
                fileID = fileID
        )
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

//    override fun getThumbnail(entry: NestedEntry): String {
//        val addon = CurseClient.getAddonByName(entry.id, entry.curseMetaUrl)!!
//        return addon.attachments?.firstOrNull { it.default }?.thumbnailUrl ?: ""
//    }

    private suspend fun resolveDependencies(addonId: Int, fileId: Int, entry: Entry, addEntry: (Entry, String) -> Unit) {
        val addon = getAddon(addonId, entry.curseMetaUrl)
        if(addon == null) {
            logger.error("addon $addonId could not be resolved, entry: $entry")
            addon!!
        }
        val addonFile = getAddonFile(addonId, fileId, entry.curseMetaUrl)
        if(addonFile == null) {
            logger.error("addon file $addonId:$fileId could not be resolved, entry: $entry")
            addonFile!!
        }
        val dependencies = addonFile.dependencies ?: return

        logger.info("dependencies of ${entry.id} ${addonFile.dependencies}")
        logger.info(entry.toString())

        for ((depAddonId, depType) in dependencies) {
            logger.info("resolve Dep $depAddonId")
            val depAddon = getAddon(depAddonId, entry.curseMetaUrl)
            if (depAddon == null) {
                logger.error("broken dependency type: '$depType' id: '$depAddonId' of entry: '${entry.id}'")
                continue
            }

//            val depends = entry.dependencies
            var dependsSet = entry.dependencies[depType]?.toSet() ?: setOf<String>()
            logger.info("get dependency $depType = $dependsSet + ${depAddon.slug}")
            if (!dependsSet.contains(depAddon.slug)) {
                val replacementSlug = entry.replaceDependencies[depAddon.slug]
                if (replacementSlug != null) {
                    if (replacementSlug.isNotBlank()) {
                        logger.info("${entry.id} adding replaced dependency ${depAddon.id} ${depAddon.slug} -> $replacementSlug")
                        dependsSet += replacementSlug
                    } else {
                        logger.info("ignoring dependency ${depAddon.id} ${depAddon.slug}")
                    }
                    continue
                }

                logger.info("${entry.id} adding dependency ${depAddon.id}  ${depAddon.slug}")
                dependsSet += depAddon.slug
            }
            entry.dependencies[depType] = dependsSet.toList()
            logger.info("set dependency $depType = $dependsSet")

            if (depType == DependencyType.REQUIRED || (entry.curseOptionalDependencies && depType == DependencyType.OPTIONAL)) {
                val depEntry = Entry(provider = Provider.CURSE.name, id = depAddon.slug).apply {
                    name = entry.name
                    side = entry.side
                    transient = true
                    curseReleaseTypes = entry.curseReleaseTypes
                    curseOptionalDependencies = entry.curseOptionalDependencies
                }
                addEntry(depEntry, depAddon.categorySection.path)
                logger.info("added $depType dependency ${depAddon.name} of ${addon.name}")
            } else {
                continue
            }

        }
    }

    private fun isOptionalCall(entry: Entry, modpack: ModPack): Boolean {
        ProviderBase.logger.info("test optional of ${entry.id}")
//        logger.info(entry.toString())
        return entry.transient || entry.optional
//        for ((depType, entryList) in entry.provides) {
//            if (depType != DependencyType.REQUIRED) continue
//            if (entryList.isEmpty()) return false
//            ProviderBase.logger.info("type: $depType list: $entryList")
//            for (entryName in entryList) {
//                val providerEntry = modpack.entries.firstOrNull { it.id == entryName }!!
//                val tmpResult = isOptional(providerEntry, modpack)
//                if (!tmpResult) return false
//            }
//        }
//        return false
    }

    val isOptional = CurseProviderThing::isOptionalCall.memoize()

    override suspend fun download(entry: LockEntry, targetFolder: File, cacheDir: File): Pair<String, File> {
        val addonFile = getAddonFile(entry.projectID, entry.fileID, entry.curseMetaUrl)
        if (addonFile == null) {
            logger.error("cannot download ${entry.id} ${entry.projectID}:${entry.fileID}")
            exitProcess(3)
        }
        val targetFile = targetFolder.resolve(entry.fileName ?: addonFile.fileNameOnDisk)
        targetFile.download(addonFile.downloadURL, cacheDir.resolve("CURSE").resolve(entry.projectID.toString()).resolve(entry.fileID.toString()))
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
}