package voodoo.builder.provider

import mu.KLogging
import voodoo.builder.curse.CurseUtil.getAddon
import voodoo.builder.curse.CurseUtil.getAddonFile
import voodoo.builder.curse.CurseUtil.findFile
import voodoo.builder.curse.DependencyType
import voodoo.builder.curse.PackageType
import voodoo.builder.data.Entry
import voodoo.builder.data.Modpack
import voodoo.builder.data.Side
import java.io.File

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */
class CurseProviderThing : ProviderThingy() {
    companion object : KLogging()

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
                    e.websiteUrl = getAddon(e.id)!!.webSiteURL
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
                { it.id > 0 && it.internal.targetPath.isBlank() },
                { e, _ ->
                    e.internal.targetPath = getAddon(e.id)!!.categorySection.path
                }
        )
        register("cacheRelpath",
                { it.internal.cacheRelpath.isBlank() },
                { e, _ ->
                    e.internal.cacheRelpath = File(e.provider.toString()).resolve(e.id.toString()).resolve(e.fileId.toString()).path
                }
        )
        register("prepareDownload",
                {
                    with(it) {
                        listOf(url, name, fileName, internal.cachePath).all { it.isNotBlank() }
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
        logger.info("dependencies of ${entry.name} ${addonFile.dependencies}")
        logger.info(entry.toString())
        for ((depAddonId, depType) in addonFile.dependencies) {

            val depAddon = getAddon(depAddonId) ?: continue

//            val depends = entry.dependencies
            var dependsList = entry.dependencies[depType] ?: listOf()
            logger.info("get dependency $depType = $dependsList + ${depAddon.name}")
            if (!dependsList.contains(depAddon.name)) {
                logger.info("${entry.name} adding dependency ${depAddon.name}")
                dependsList += depAddon.name
            }
            entry.dependencies[depType] = dependsList
            logger.info("set dependency $depType = $dependsList")

            // find duplicate entry
            var depEntry = modpack.mods.entries.firstOrNull { e ->
                e.provider == Provider.CURSE &&
                        (e.id == depAddon.id || e.name == depAddon.name)
            }
            if (depEntry == null) {
                if (depType == DependencyType.required || (entry.doOptionals && depType == DependencyType.optional)) {
                    depEntry = Entry(
                            provider = Provider.CURSE,
                            id = depAddon.id,
                            name = depAddon.name,
                            side = entry.side,
                            transient = true,
                            doOptionals = entry.doOptionals
                    )
                    modpack.mods.entries += depEntry
                    logger.info("added $depType dependency ${depAddon.name} of ${addon.name}")
                } else {
                    return
                }
            } else {
                val otherSide = depEntry.side
                val side = Side.values().find { s -> s.flag == otherSide.flag or entry.side.flag } ?: throw Exception("invalid side")

                depEntry.side = side
            }

            // moved to ProviderThing as postResolveDependencies
//            var provideList = depEntry.provides[depType] ?: emptyList()
//            provideList += addon.name
//            depEntry.provides[depType] = provideList
        }
        entry.internal.resolvedDependencies = true
    }



}