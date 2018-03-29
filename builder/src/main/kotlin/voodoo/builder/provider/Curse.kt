package voodoo.builder.provider

import aballano.kotlinmemoization.memoize
import mu.KLogging
import voodoo.core.curse.CurseUtil.getAddon
import voodoo.core.curse.CurseUtil.getAddonFile
import voodoo.core.curse.CurseUtil.findFile
import voodoo.core.curse.DependencyType
import voodoo.core.data.flat.Entry
import voodoo.core.data.flat.ModPack
import voodoo.core.data.lock.LockEntry

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */
class CurseProviderThing : ProviderBase {
    override val name = "Curse Provider"

    companion object : KLogging()

    override fun resolve(entry: Entry, modpack: ModPack, addEntry: (Entry) -> Unit): LockEntry {
        val (projectID, fileID, _) = findFile(entry, modpack.mcVersion, entry.curseMetaUrl)

        resolveDependencies(projectID, fileID, entry, modpack, addEntry)

        entry.optional = isOptional(entry, modpack)

        return LockEntry(entry.provider, projectID = projectID, fileID = fileID)
    }

    private fun resolveDependencies(addonId: Int, fileId: Int, entry: Entry, modpack: ModPack, addEntry: (Entry) -> Unit) {
        val addon = getAddon(addonId, entry.curseMetaUrl)!!
        val addonFile = getAddonFile(addonId, fileId, entry.curseMetaUrl)!!
        if(addonFile.dependencies == null) return
        logger.info("dependencies of ${entry.name} ${addonFile.dependencies}")
        logger.info(entry.toString())
        for ((depAddonId, depType) in addonFile.dependencies!!) {
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
                val depEntry = Entry().apply {
                    provider = Provider.CURSE.toString()
                    //id = depAddon.id,
                    name = depAddon.name
                    side = entry.side
                    transient = true
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

}