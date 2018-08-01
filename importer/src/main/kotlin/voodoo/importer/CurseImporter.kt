package voodoo.importer

import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonObject
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import voodoo.curse.CurseClient
import voodoo.data.curse.CurseConstancts.PROXY_URL
import voodoo.data.curse.CurseManifest
import voodoo.data.curse.FileType
import voodoo.data.flat.Entry
import voodoo.data.flat.EntryFeature
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.provider.Provider
import voodoo.registerSerializer
import voodoo.registerTypeAdapter
import voodoo.util.UnzipUtility.unzip
import voodoo.util.readJson
import voodoo.util.writeYaml
import java.io.File
import kotlin.system.exitProcess

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

    override suspend fun import(source: String, target: File) {
        val name = target.nameWithoutExtension
        val zipFile = directories.cacheHome.resolve("$name.zip")
        zipFile.deleteRecursively()
        val (request, response, result) = source.httpGet().response()
        when (result) {
            is Result.Success -> {
                zipFile.parentFile.mkdirs()
                zipFile.writeBytes(result.value)
            }
            is Result.Failure -> {
                logger.error("invalid statusCode {} from {}", response.statusCode, source)
                logger.error("connection url: {}", request.url)
                logger.error("content: {}", result.component1())
                logger.error("error: {}", result.error.toString())
                exitProcess(1)
            }
        }

        val extractFolder = directories.cacheHome.resolve(target.nameWithoutExtension)
        unzip(zipFile.absolutePath, extractFolder.absolutePath)

        val manifest = extractFolder.resolve("manifest.json").readJson<CurseManifest>()

        val validMcVersions = mutableSetOf<String>()

        val overridesFolder = File(name).resolve(manifest.overrides)
        overridesFolder.deleteRecursively()
        overridesFolder.mkdirs()

        extractFolder.resolve(manifest.overrides).copyRecursively(overridesFolder)

        //TODO: copy content of mods into local and add local entries

        val modsFolder = overridesFolder.resolve("mods")

        val entries = manifest.files.map {
            logger.info { it }
            val addon = CurseClient.getAddon(it.projectID, PROXY_URL)!!
            val addonFile = CurseClient.getAddonFile(it.projectID, it.fileID, PROXY_URL)!!
            val nestedEntry = NestedEntry(
//                    provider = Provider.CURSE.id,
                    id = addon.slug,
                    version = addonFile.fileName
            )

            validMcVersions += addonFile.gameVersion

            val entry = Entry(
                    provider = Provider.CURSE.name,
                    curseReleaseTypes = setOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA),
                    id = addon.slug,
                    fileName = addonFile.fileName,
                    validMcVersions = addonFile.gameVersion.toSet()
            )
            val json = YamlImporter.jankson.toJson(entry)//.toJson(true, true)
            if (json is JsonObject) {
                val defaultJson = entry.toDefaultJson(YamlImporter.jankson.marshaller)
                val delta = json.getDelta(defaultJson)
                modsFolder.resolve("${addon.slug}.entry.hjson").writeText(delta.toJson(true, true).replace("\t", "  "))
            }

            val (projectID, fileID, path) = CurseClient.findFile(entry, manifest.minecraft.version, PROXY_URL)

            val lockEntry = LockEntry(
                    provider = "CURSE",
                    id = nestedEntry.id,
                    //folder = path, //maybe use entry.folder only if its non-default
                    useUrlTxt = true,
                    projectID = projectID,
                    fileID = fileID
            )
            val lockJson = YamlImporter.jankson.toJson(lockEntry)//.toJson(true, true)
            if (lockJson is JsonObject) {
                val defaultJson = lockEntry.toDefaultJson(YamlImporter.jankson.marshaller)
                val delta = lockJson.getDelta(defaultJson)
                modsFolder.resolve("${addon.slug}.lock.json").writeText(delta.toJson(true, true).replace("\t", "  "))
            }
            overridesFolder.resolve(path).apply { mkdirs() }.resolve("${addon.slug}.lock.json").writeText(
                    jankson.toJson(lockEntry).toJson(true, true).replace("\t", "    ")
            )

            nestedEntry
        }


        val forge = manifest.minecraft.modLoaders
                .find { it.id.startsWith("forge-") }?.id?.substringAfterLast('.')
        val nestedPack = NestedPack(
                name,
                authors = listOf(manifest.author),
                title = manifest.name,
                version = manifest.version,
                forge = forge ?: "recommended",
                mcVersion = manifest.minecraft.version,
                sourceDir = overridesFolder.relativeTo(File(name)).path,
                root = NestedEntry(
                        validMcVersions = validMcVersions - manifest.minecraft.version,
                        provider = Provider.CURSE.name,
                        curseReleaseTypes = setOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA),
                        entries = entries
                )
        )

        val filename = manifest.name.replace("[^\\w-]+".toRegex(), "")
        val packFile = target.resolve("$filename.pack.hjson")
        val lockFile = target.resolve("$filename.lock.json")

        target.resolve("$filename.yaml").writeYaml(nestedPack)

        val modpack = nestedPack.flatten()

        val json = YamlImporter.jankson.toJson(modpack) as JsonObject
        val defaultJson = modpack.toDefaultJson(YamlImporter.jankson.marshaller)
        val delta = json.getDelta(defaultJson)
        packFile.writeText(delta.toJson(true, true).replace("\t", "    "))

        val lockedPack = modpack.lock()

        val lockJson = jankson.toJson(lockedPack) as JsonObject
        lockFile.writeText(lockJson.toJson(false, true).replace("\t", "    "))

        //TODO: create srcDir with single entries and version-locked files and ModPack
    }
}