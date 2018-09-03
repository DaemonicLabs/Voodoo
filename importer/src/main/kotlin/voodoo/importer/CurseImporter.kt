package voodoo.importer

import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonObject
import kotlinx.coroutines.*
import voodoo.curse.CurseClient
import voodoo.data.curse.CurseConstancts.PROXY_URL
import voodoo.data.curse.CurseManifest
import voodoo.data.curse.FileType
import voodoo.data.flat.Entry
import voodoo.data.flat.EntryFeature
import voodoo.data.lock.LockEntry
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.provider.Provider
import voodoo.registerSerializer
import voodoo.registerTypeAdapter
import voodoo.util.UnzipUtility.unzip
import voodoo.util.blankOr
import voodoo.util.download
import voodoo.util.readJson
import voodoo.util.writeYaml
import java.io.File
import java.util.*
import kotlin.coroutines.coroutineContext

/**
 * Created by nikky on 13/06/18.
 * @author Nikky
 */

object CurseImporter : AbstractImporter() {
    override val label = "Curse Importer"

    private val jankson = Jankson.builder()
            .registerTypeAdapter(Entry.Companion::fromJson)
            .registerTypeAdapter(EntryFeature.Companion::fromJson)
            .registerSerializer(Entry.Companion::toJson)
//            .registerSerializer(EntryFeature.Companion::toJson)
            .build()

    override suspend fun import(source: String, target: File, name: String?) {
        val tmpName = name.blankOr ?: UUID.randomUUID().toString()

        val cacheHome = directories.cacheHome.resolve("IMPORT")
        val zipFile = cacheHome.resolve("$tmpName.zip")
        zipFile.deleteRecursively()
        zipFile.download(source, directories.cacheHome.resolve("DIRECT"))

        val extractFolder = cacheHome.resolve(tmpName)
        unzip(zipFile.absolutePath, extractFolder.absolutePath)

        val manifest = extractFolder.resolve("manifest.json").readJson<CurseManifest>()

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
                file.name.endsWith(".entry.lock.json") -> false
                else -> true
            }
        }.forEach { file ->
            if(!file.isFile) return@forEach
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

        if(localEntries.isNotEmpty()) {
            entries += NestedEntry(
                    provider = Provider.LOCAL.name,
                    entries = localEntries
            )
        }

        val pool = newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors() + 1, "pool")
        val jobs = mutableListOf<Job>()

        //TODO: process in parallel
        for(file in manifest.files) {
            jobs += launch(context = coroutineContext + pool) {
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
                        provider = Provider.CURSE.name,
                        curseReleaseTypes = sortedSetOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA),
                        id = addon.slug,
                        fileName = addonFile.fileName,
                        validMcVersions = addonFile.gameVersion.toSet()
                )
                val json = YamlImporter.jankson.toJson(entry)//.toJson(true, true)

                val (projectID, fileID, path) = CurseClient.findFile(entry, manifest.minecraft.version, PROXY_URL)

                if (json is JsonObject) {
                    val defaultJson = entry.toDefaultJson(YamlImporter.jankson.marshaller)
                    val delta = json.getDelta(defaultJson)
                    overridesFolder.resolve(path).apply { mkdirs() }
                            .resolve("${addon.slug}.entry.hjson").writeText(
                                    delta.toJson(true, true).replace("\t", "  ")
                            )
                }


                val lockEntry = LockEntry(
                        provider = "CURSE",
                        id = nestedEntry.id,
                        useUrlTxt = true,
                        projectID = projectID,
                        fileID = fileID
                )
                val lockJson = YamlImporter.jankson.toJson(lockEntry)//.toJson(true, true)
                if (lockJson is JsonObject) {
                    val defaultJson = lockEntry.toDefaultJson(YamlImporter.jankson.marshaller)
                    val delta = lockJson.getDelta(defaultJson)
                    overridesFolder.resolve(path).apply { mkdirs() }.resolve("${addon.slug}.entry.lock.json").writeText(
                            delta.toJson(true, true).replace("\t", "    ")
                    )
                }

                entries += nestedEntry
            }
            delay(10)
        }

        delay(10)
        logger.info("waiting for jobs to finish")
        runBlocking { jobs.forEach { it.join() } }

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
                root = NestedEntry(
                        include = entriesFilename
                )
        )
        val rootEntry = NestedEntry(
                validMcVersions = validMcVersions - manifest.minecraft.version,
                provider = Provider.CURSE.name,
                curseReleaseTypes = sortedSetOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA),
                entries = entries
        )


        logger.info("writing to $mainFilename.yaml")
        target.resolve("$mainFilename.yaml").writeYaml(nestedPack)
        logger.info("writing to $entriesFilename")
        target.resolve(entriesFilename).writeYaml(rootEntry)

        val packFile = target.resolve("$mainFilename.pack.hjson")
        val lockFile = target.resolve("$mainFilename.lock.json")

        val modpack = nestedPack.flatten()

        val json = YamlImporter.jankson.toJson(modpack) as JsonObject
        val defaultJson = modpack.toDefaultJson(YamlImporter.jankson.marshaller)
        val delta = json.getDelta(defaultJson)
        packFile.writeText(delta.toJson(true, true).replace("\t", "    "))

        val lockedPack = modpack.lock()

        val lockJson = jankson.toJson(lockedPack) as JsonObject
        lockFile.writeText(lockJson.toJson(false, true).replace("\t", "    "))

        //TODO: create srcDir with single entries and version-locked files and ModPack

        extractFolder.deleteRecursively()
        zipFile.delete()
    }

    //TODO: options filename, src-folder/overrides,
}