package voodoo.importer

import fileSrc
import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.serialization.json.JSON
import list
import releaseTypes
import voodoo.MainEnv
import voodoo.NewModpack
import voodoo.Poet
import voodoo.curse.CurseClient
import voodoo.data.curse.CurseConstants.PROXY_URL
import voodoo.data.curse.CurseManifest
import voodoo.data.curse.FileType
import voodoo.data.curse.ProjectID
import voodoo.provider.CurseProvider
import voodoo.provider.LocalProvider
import voodoo.util.UnzipUtility.unzip
import voodoo.util.blankOr
import voodoo.util.download
import voodoo.util.pool
import withProvider
import java.io.File
import java.util.UUID

/**
 * Created by nikky on 13/06/18.
 * @author Nikky
 */

object CurseImporter : AbstractImporter() {
    override val label = "Curse Importer"

    suspend fun import(
        modpackId: String,
        sourceUrl: String,
        rootDir: File,
        packsDir: File
    ) {
//        Thread.currentThread().contextClassLoader = CurseImporter::class.java.classLoader
        val tmpName = modpackId.blankOr ?: UUID.randomUUID().toString()

        val cacheHome = directories.cacheHome.resolve("IMPORT")
        val zipFile = cacheHome.resolve("$tmpName.zip")
        zipFile.deleteRecursively()
        zipFile.download(sourceUrl, directories.cacheHome.resolve("DIRECT"))

        val extractFolder = cacheHome.resolve(tmpName)
        unzip(zipFile.absolutePath, extractFolder.absolutePath)

        val manifest = JSON.parse<CurseManifest>(extractFolder.resolve("manifest.json").readText())

        val validMcVersions = mutableSetOf<String>()

        val source = modpackId
        val local = "local"
        val sourceFolder = rootDir.resolve(source)
        sourceFolder.deleteRecursively()
        sourceFolder.mkdirs()

        extractFolder.resolve(manifest.overrides).copyRecursively(sourceFolder)

//        val pool = newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors() + 1, "pool")

        val curseChannel = Channel<Triple<String, String, ProjectID>>(Channel.UNLIMITED)

        coroutineScope {
            for (file in manifest.files) {
                launch(context = pool + CoroutineName("${file.projectID}:${file.fileID}")) {
                    logger.info { file }
                    val addon = CurseClient.getAddon(file.projectID, PROXY_URL)!!
                    val addonFile = CurseClient.getAddonFile(file.projectID, file.fileID, PROXY_URL)!!

                    if (addonFile.gameVersion.none { version -> validMcVersions.contains(version) }) {
                        validMcVersions += addonFile.gameVersion
                    }

                    curseChannel.send(
                        Triple(
                            Poet.defaultSlugSanitizer(addon.slug),
                            Regex.escape(addonFile.fileName),
                            addon.id
                        )
                    )
                }
                delay(10)
            }

            delay(10)
            logger.info("waiting for jobs to finish")
        }
        logger.info("jobs finished")
        curseChannel.close()

        val curseEntries = mutableListOf<Triple<String, String, ProjectID>>()
        for (curseEntry in curseChannel) {
            curseEntries += curseEntry
        }

//        val forge = manifest.minecraft.modLoaders
//            .find { it.id.startsWith("forge-") }?.id?.substringAfterLast('.')

        val nestedPack = MainEnv(
            rootDir = rootDir
        ).nestedPack(
            id = modpackId,
            mcVersion = manifest.minecraft.version
        ) {
            authors = listOf(manifest.author)
            title = manifest.name
            version = manifest.version
            // TODO pick correct forge version number
//            forge = null
            sourceDir = source
            localDir = local
            root = rootEntry(CurseProvider) {
                this.validMcVersions = validMcVersions - manifest.minecraft.version
                releaseTypes = sortedSetOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA)
                list {
                    curseEntries.forEach { (identifier, versionStr, curseProjectID) ->
                        add(curseProjectID.value) {
                            version = versionStr
                        }
                    }
                    val modsFolder = sourceFolder.resolve("mods")
                    if (modsFolder.exists()) {
                        withProvider(LocalProvider).list {
                            val localFolder = rootDir.resolve(local)
                            logger.info("listing $modsFolder")
                            modsFolder.listFiles { file ->
                                logger.debug("testing $file")
                                when {
                                    file == null -> false
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

                                id(file.nameWithoutExtension) {
                                    fileSrc = relative.path
                                    folder = file.parentFile.relativeTo(sourceFolder).path
                                }
                                file.delete()
                            }
                        }
                    }
                }
            }
        }

        NewModpack.createModpack(
            folder = packsDir,
            nestedPack = nestedPack
        )

//        val modpack = Importer.flatten(nestedPack)
//        val lockPack = Builder.build(modpack, name = modpackId, args = *arrayOf("build"))
//        Tome.generate(modpack, lockPack, mainEnv.tomeEnv)
//        logger.info("finished")

        extractFolder.deleteRecursively()
        zipFile.delete()
    }

    // TODO: options filename, src-folder/overrides,
}