package voodoo.importer

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import voodoo.curse.CurseClient
import voodoo.data.curse.CurseConstancts.PROXY_URL
import voodoo.data.curse.CurseManifest
import voodoo.data.curse.FileType
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.provider.Provider
import voodoo.util.UnzipUtility.unzip
import voodoo.util.readJson
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 13/06/18.
 * @author Nikky
 */

object CurseImporter : AbstractImporter() {
    override val label = "Curse Importer"

    override fun import(source: String, target: File): Pair<ModPack?, MutableMap<String, LockEntry>?> {
        val versions = mutableMapOf<String, LockEntry>()

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

        val entries = manifest.files.map {
            logger.info { it }
            val addon = CurseClient.getAddon(it.projectID, PROXY_URL)!!
            val addonFile = CurseClient.getAddonFile(it.projectID, it.fileID, PROXY_URL)!!
            val entry = NestedEntry(
                    provider = Provider.CURSE.name,
                    name = addon.name,
                    version = addonFile.fileName
            )
            validMcVersions += addonFile.gameVersion

            val flatEntry = Entry(
                    provider = Provider.CURSE.name,
                    curseReleaseTypes = setOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA),
                    name = addon.name,
                    fileName = addonFile.fileName,
                    validMcVersions = addonFile.gameVersion.toSet()
            )

            val (projectID, fileID, path) = CurseClient.findFile(flatEntry, manifest.minecraft.version, PROXY_URL)

            versions[addon.name] = LockEntry(
                    provider = "CURSE",
                    name = entry.name,
                    //folder = path, //maybe use entry.folder only if its non-default
                    useUrlTxt = true,
                    projectID = projectID,
                    fileID = fileID
            )

            entry
        }

        val overridesFolder = File(name).resolve(manifest.overrides)
        overridesFolder.deleteRecursively()
        overridesFolder.mkdirs()

        extractFolder.resolve(manifest.overrides).copyRecursively(overridesFolder)

        val forge = manifest.minecraft.modLoaders
                .find { it.id.startsWith("forge-") }?.id?.substringAfterLast('.')
        val pack = NestedPack(
                manifest.name,
                authors = listOf(manifest.author),
                title = manifest.name,
                version = manifest.version,
                forge = forge ?: "recommended",
                mcVersion = manifest.minecraft.version,
                sourceDir = overridesFolder.path,
                root = NestedEntry(
                        validMcVersions = validMcVersions,
                        provider = "CURSE",
                        curseReleaseTypes = setOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA),
                        entries = entries
                )
        )

        //TODO: create srcDir with single entries and version-locked files and ModPack

        return Pair(null, null)
    }
}