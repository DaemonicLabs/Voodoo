package voodoo.importer

import kotlinx.coroutines.experimental.*
import kotlinx.serialization.json.JSON
import voodoo.curse.CurseClient
import voodoo.data.curse.CurseConstancts.PROXY_URL
import voodoo.data.curse.CurseManifest
import voodoo.data.curse.FileType
import voodoo.data.flat.Entry
import voodoo.data.lock.LockEntry
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.provider.CurseProvider
import voodoo.provider.LocalProvider
import voodoo.util.UnzipUtility.unzip
import voodoo.util.blankOr
import voodoo.util.download
import voodoo.util.toJson
import java.io.File
import java.util.*

/**
 * Created by nikky on 13/06/18.
 * @author Nikky
 */

@Deprecated("roken due to significant changes to the codebase and deprecation of yaml")
object CurseImporter : AbstractImporter() {
    override val label = "Curse Importer"

    override suspend fun import(
        source: String,
        target: File,
        name: String?
    ) {
        val tmpName = name.blankOr ?: UUID.randomUUID().toString()

        val cacheHome = directories.cacheHome.resolve("IMPORT")
        val zipFile = cacheHome.resolve("$tmpName.zip")
        zipFile.deleteRecursively()
        zipFile.download(source, directories.cacheHome.resolve("DIRECT"))

        val extractFolder = cacheHome.resolve(tmpName)
        unzip(zipFile.absolutePath, extractFolder.absolutePath)

        val manifest = JSON.parse<CurseManifest>(extractFolder.resolve("manifest.json").readText())

        val validMcVersions = mutableSetOf<String>()

        val overrides = "src"
        val local = "local"
        val overridesFolder = target.resolve(overrides)
        overridesFolder.deleteRecursively()
        overridesFolder.mkdirs()

        extractFolder.resolve(manifest.overrides).copyRecursively(overridesFolder)

        val modsFolder = overridesFolder.resolve("mods")
        val localFolder = target.resolve(local)
        val localEntries = mutableListOf<NestedEntry>()

        logger.info("listing $modsFolder")
        modsFolder.listFiles { file ->
            logger.debug("testing $file")
            when {
                !file.isFile -> false
                file.name.endsWith(".entry.hjson") -> false
                file.name.endsWith(".lock.hjson") -> false
                else -> true
            }
        }.forEach { file ->
            if (!file.isFile) return@forEach
            val relative = file.relativeTo(modsFolder)
            val targetFile = localFolder.resolve(relative)
            targetFile.parentFile.mkdirs()
            file.copyTo(targetFile, overwrite = true)
            logger.info("adding local entry for ${relative.path}")
            localEntries += NestedEntry(
                id = file.nameWithoutExtension,
                fileSrc = relative.path,
                folder = file.parentFile.relativeTo(overridesFolder).path
            )
            file.delete()
        }

        val entries = mutableListOf<NestedEntry>()

        if (localEntries.isNotEmpty()) {
            entries += NestedEntry(
                provider = LocalProvider.id,
                entries = localEntries
            )
        }

        val pool = newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors() + 1, "pool")
        coroutineScope {
            val jobs = mutableListOf<Job>()
            //TODO: process in parallel
            for (file in manifest.files) {
                jobs += launch(context = pool) {
                    logger.info { file }
                    val addon = CurseClient.getAddon(file.projectID, PROXY_URL)!!
                    val addonFile = CurseClient.getAddonFile(file.projectID, file.fileID, PROXY_URL)!!
                    val nestedEntry = NestedEntry(
                        id = addon.slug,
                        version = addonFile.fileName
                    )

                    if (addonFile.gameVersion.none { version -> validMcVersions.contains(version) }) {
                        validMcVersions += addonFile.gameVersion
                    }

                    val entry = Entry(
                        provider = CurseProvider.id,
                        curseReleaseTypes = sortedSetOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA),
                        id = addon.slug,
                        fileName = addonFile.fileName,
                        validMcVersions = addonFile.gameVersion.toSet()
                    )
                    val (projectID, fileID, path) = CurseClient.findFile(entry, manifest.minecraft.version, PROXY_URL)

                    overridesFolder.resolve(path).apply { mkdirs() }
                        .resolve("${addon.slug}.entry.hjson").writeText(
                            entry.toJson
                        )

                    val lockEntry = LockEntry(
                        provider = "CURSE",
                        id = nestedEntry.id,
                        useUrlTxt = true,
                        projectID = projectID,
                        fileID = fileID
                    )
                    overridesFolder.resolve(path).apply { mkdirs() }.resolve("${addon.slug}.lock.hjson")
                        .writeText(lockEntry.toJson)


                    entries += nestedEntry
                }
                delay(10)
            }

            delay(10)
            logger.info("waiting for jobs to finish")
            jobs.forEach { it.join() }
        }

        val forge = manifest.minecraft.modLoaders
            .find { it.id.startsWith("forge-") }?.id?.substringAfterLast('.')

        val mainFilename = name.blankOr ?: manifest.name.replace("[^\\w-]+".toRegex(), "")
        val entriesFilename = mainFilename + "_entries.yaml"

        val nestedPack = NestedPack(
            name ?: manifest.name.replace("[^\\w-]+".toRegex(), ""),
            authors = listOf(manifest.author),
            title = manifest.name,
            version = manifest.version,
            forge = forge ?: "recommended",
            mcVersion = manifest.minecraft.version,
            sourceDir = overridesFolder.relativeTo(target).path,
            localDir = local,
            root = NestedEntry(/*include = entriesFilename*/)
        )
        val rootEntry = NestedEntry(
            validMcVersions = validMcVersions - manifest.minecraft.version,
            provider = CurseProvider.id,
            curseReleaseTypes = sortedSetOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA),
            entries = entries
        )


//        logger.info("writing to $mainFilename.yaml")
//        target.resolve("$mainFilename.yaml").writeYaml(nestedPack)
//        logger.info("writing to $entriesFilename")
//        target.resolve(entriesFilename).writeYaml(rootEntry)

        val packFile = target.resolve("$mainFilename.pack.hjson")
        val lockFile = target.resolve("$mainFilename.lock.hjson")

        val modpack = nestedPack.flatten()

        packFile.writeText(modpack.toJson)

        val lockedPack = modpack.lock()

        lockFile.writeText(lockedPack.toJson)

        //TODO: create srcDir with single entries and version-locked files and ModPack

        extractFolder.deleteRecursively()
        zipFile.delete()
    }

    //TODO: options filename, src-folder/overrides,
}