package voodoo.provider.impl

import aballano.kotlinmemoization.memoize
import mu.KLogging
import voodoo.curse.CurseUtil
import voodoo.curse.CurseUtil.findFile
import voodoo.curse.CurseUtil.getAddon
import voodoo.curse.CurseUtil.getAddonFile
import voodoo.curse.DependencyType
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.provider.Provider
import voodoo.provider.ProviderBase
import voodoo.util.download
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */
class CurseProviderThing : ProviderBase {
    override val name = "Curse Provider"

    companion object : KLogging()

    override fun resolve(entry: Entry, modpack: ModPack, addEntry: (Entry) -> Unit): LockEntry {
        val (projectID, fileID, path) = findFile(entry, modpack.mcVersion, modpack.curseMetaUrl)

        resolveDependencies(projectID, fileID, entry, modpack, addEntry)

        entry.optional = isOptional(entry, modpack)

        return LockEntry(
                provider = entry.provider,
                name = entry.name,
                useUrlTxt = entry.useUrlTxt,
                fileName = entry.fileName,
                side = entry.side,
                projectID = projectID,
                fileID = fileID,
                folder = path
        )
    }

    override fun getAuthors(entry: LockEntry, modpack: LockPack): List<String> {
        return CurseUtil.getAuthors(entry.projectID, modpack.curseMetaUrl)
    }

    override fun getProjectPage(entry: LockEntry, modpack: LockPack): String {
        return CurseUtil.getProjectPage(entry.projectID, modpack.curseMetaUrl)
    }

    override fun getVersion(entry: LockEntry, modpack: LockPack): String {
        val addonFile = getAddonFile(entry.projectID, entry.fileID, modpack.curseMetaUrl)
        return addonFile?.fileName ?: ""
    }

    private fun resolveDependencies(addonId: Int, fileId: Int, entry: Entry, modpack: ModPack, addEntry: (Entry) -> Unit) {
        val addon = getAddon(addonId, modpack.curseMetaUrl)!!
        val addonFile = getAddonFile(addonId, fileId, modpack.curseMetaUrl)!!
        if (addonFile.dependencies == null) return
        logger.info("dependencies of ${entry.name} ${addonFile.dependencies}")
        logger.info(entry.toString())
        for ((depAddonId, depType) in addonFile.dependencies!!) {
            logger.info("resolve Dep $depAddonId")
            val depAddon = getAddon(depAddonId, modpack.curseMetaUrl) ?: continue

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
                val depEntry = Entry().apply {
                    provider = Provider.CURSE.toString()
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
        var result = entry.transient || entry.optional
        if (result) return result
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
        return true
    }

    val isOptional = ::isOptionalCall.memoize()

    override fun download(entry: LockEntry, modpack: LockPack, target: File, cacheDir: File): Pair<String, File> {
        val addonFile = getAddonFile(entry.projectID, entry.fileID, modpack.curseMetaUrl)
        if (addonFile == null) {
            logger.error("cannot download ${entry.projectID}:${entry.fileID}")
            exitProcess(3)
        }
        val targetFile = target.resolve(entry.fileName ?: addonFile.fileNameOnDisk)
        targetFile.download(addonFile.downloadURL, cacheDir.resolve("CURSE").resolve(entry.projectID.toString()).resolve(entry.fileID.toString()))
        return Pair(addonFile.downloadURL, targetFile)
    }
}