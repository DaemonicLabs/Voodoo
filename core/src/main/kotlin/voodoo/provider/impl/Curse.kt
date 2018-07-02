package voodoo.provider.impl

import aballano.kotlinmemoization.memoize
import mu.KLogging
import voodoo.curse.CurseClient
import voodoo.curse.CurseClient.findFile
import voodoo.curse.CurseClient.getAddon
import voodoo.curse.CurseClient.getAddonFile
import voodoo.data.curse.DependencyType
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.provider.Provider
import voodoo.provider.ProviderBase
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

    override fun resolve(entry: Entry, modpack: ModPack, addEntry: (Entry) -> Unit): LockEntry {
        val (projectID, fileID, path) = findFile(entry, modpack.mcVersion, entry.curseMetaUrl)

        resolveDependencies(projectID, fileID, entry, addEntry)

        entry.optional = isOptional(entry, modpack)

        return LockEntry(
                provider = entry.provider,
                curseMetaUrl = entry.curseMetaUrl,
                name = entry.name,
                //folder = path, //maybe use entry.folder only if its non-default
                useUrlTxt = entry.useUrlTxt,
                fileName = entry.fileName,
                side = entry.side,
                projectID = projectID,
                fileID = fileID
        )
    }

    override fun getAuthors(entry: LockEntry): List<String> {
        return CurseClient.getAuthors(entry.projectID, entry.curseMetaUrl)
    }

    override fun getProjectPage(entry: LockEntry): String {
        return "https://minecraft.curseforge.com/projects/${entry.projectID}"
        //CurseClient.getProjectPage(entry.projectID, modpack.curseMetaUrl)
    }

    override fun getVersion(entry: LockEntry): String {
        val addonFile = getAddonFile(entry.projectID, entry.fileID, entry.curseMetaUrl)
        return addonFile?.fileName ?: ""
    }

    override fun getLicense(entry: LockEntry): String {
        return "https://minecraft.curseforge.com/projects/${entry.fileID}/license"
    }

    override fun getThumbnail(entry: LockEntry): String {
        val addon = CurseClient.getAddon(entry.projectID, entry.curseMetaUrl)
        return addon?.attachments?.firstOrNull { it.default }?.thumbnailUrl ?: ""
    }

    override fun getThumbnail(entry: Entry): String {
        val addon = CurseClient.getAddonByName(entry.name, entry.curseMetaUrl)
        return addon?.attachments?.firstOrNull { it.default }?.thumbnailUrl ?: ""
    }

//    override fun getThumbnail(entry: NestedEntry): String {
//        val addon = CurseClient.getAddonByName(entry.name, entry.curseMetaUrl)!!
//        return addon.attachments?.firstOrNull { it.default }?.thumbnailUrl ?: ""
//    }

    private fun resolveDependencies(addonId: Int, fileId: Int, entry: Entry, addEntry: (Entry) -> Unit) {
        val addon = getAddon(addonId, entry.curseMetaUrl)!!
        val addonFile = getAddonFile(addonId, fileId, entry.curseMetaUrl)!!
        val dependencies = addonFile.dependencies ?: return

        logger.info("dependencies of ${entry.name} ${addonFile.dependencies}")
        logger.info(entry.toString())

        for ((depAddonId, depType) in dependencies) {
            logger.info("resolve Dep $depAddonId")
            val depAddon = getAddon(depAddonId, entry.curseMetaUrl) ?: continue

//            val depends = entry.dependencies
            var dependsList = entry.dependencies[depType] ?: listOf<String>()
            logger.info("get dependency $depType = $dependsList + ${depAddon.name}")
            if (!dependsList.contains(depAddon.name)) {
                logger.info("${entry.name} adding dependency ${depAddon.name}")
                dependsList += depAddon.name
            }
            entry.dependencies[depType] = dependsList
            logger.info("set dependency $depType = $dependsList")

            if (depType == DependencyType.REQUIRED || (entry.curseOptionalDependencies && depType == DependencyType.OPTIONAL)) {
                val depEntry = Entry(provider = Provider.CURSE.name).apply {
//                    provider = Provider.CURSE.toString()
                    //id = depAddon.id,
                    name = depAddon.name
                    side = entry.side
                    transient = true
                    curseReleaseTypes = entry.curseReleaseTypes
                    curseOptionalDependencies = entry.curseOptionalDependencies
                }
                addEntry(depEntry)
                logger.info("added $depType dependency ${depAddon.name} of ${addon.name}")
            } else {
                continue
            }

        }
    }

    private fun isOptionalCall(entry: Entry, modpack: ModPack): Boolean {
        ProviderBase.logger.info("test optional of ${entry.name}")
//        logger.info(entry.toString())
        return entry.transient || entry.optional
//        for ((depType, entryList) in entry.provides) {
//            if (depType != DependencyType.REQUIRED) continue
//            if (entryList.isEmpty()) return false
//            ProviderBase.logger.info("type: $depType list: $entryList")
//            for (entryName in entryList) {
//                val providerEntry = modpack.entries.firstOrNull { it.name == entryName }!!
//                val tmpResult = isOptional(providerEntry, modpack)
//                if (!tmpResult) return false
//            }
//        }
//        return false
    }

    val isOptional = CurseProviderThing::isOptionalCall.memoize()

    override fun download(entry: LockEntry, targetFolder: File, cacheDir: File): Pair<String, File> {
        val addonFile = getAddonFile(entry.projectID, entry.fileID, entry.curseMetaUrl)
        if (addonFile == null) {
            logger.error("cannot download ${entry.projectID}:${entry.fileID}")
            exitProcess(3)
        }
        val targetFile = targetFolder.resolve(entry.fileName ?: addonFile.fileNameOnDisk)
        targetFile.download(addonFile.downloadURL, cacheDir.resolve("CURSE").resolve(entry.projectID.toString()).resolve(entry.fileID.toString()))
        return Pair(addonFile.downloadURL, targetFile)
    }

    override fun getReleaseDate(entry: LockEntry): Instant? {
        val addonFile = getAddonFile(entry.projectID, entry.fileID, entry.curseMetaUrl)
        return when(addonFile) {
            null -> return null
            else -> {
                addonFile.fileDate.toInstant()
            }
        }
    }
}