package voodoo.builder.curse

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import khttp.get
import mu.KLogging
import voodoo.builder.data.Entry
import voodoo.builder.data.Modpack
import voodoo.builder.provider.Provider
import voodoo.builder.writeToFile
import voodoo.util.Directories
import voodoo.util.UnzipUtility
import java.io.File

/**
 * Created by nikky on 30/01/18.
 * @author Nikky
 * @version 1.0
 */
object Importer : KLogging() {
    fun import(packDefinition: File, import: String, workingDirectory: File, outPath: File) {
        val directories = Directories.get(moduleNam = "importer")
        val packFile = with(File(import)) {
            if (isFile) {
                logger.info("importing {}", this)
                this
            } else {
                val tmpFile = directories.cacheHome.resolve(import.substringAfterLast("/"))
                logger.info("downloading pack from {} to {}", import, tmpFile)
                val r = get(import, stream = true)
                if (tmpFile.exists())
                    tmpFile.delete()
                tmpFile.writeBytes(r.content)
                tmpFile
            }
        }
        logger.info("loading pack from {}", packFile)

        val tmpFolder = directories.cacheHome.resolve(packFile.nameWithoutExtension)
        if (tmpFolder.exists())
            tmpFolder.deleteRecursively()

        logger.info("extracting pack into {}", tmpFolder)
        UnzipUtility.unzip(packFile.path, tmpFolder.path)

        logger.info("extracted pack into {}", tmpFolder)

        val mapper = jacksonObjectMapper() // Enable JSON parsing
        mapper.registerModule(KotlinModule()) // Enable Kotlin support
        val manifest = tmpFolder.resolve("manifest.json").bufferedReader().use {
            mapper.readValue<CurseManifest>(it)
        }

        val packDef = if (packDefinition.path == workingDirectory.path) {
            workingDirectory.resolve(manifest.name + ".yaml")
        } else packDefinition

        val srcDir = outPath.resolve(manifest.name).resolve("src")
        if (srcDir.exists())
            srcDir.delete()
        srcDir.mkdirs()

        val overridesFolder = tmpFolder.resolve(manifest.overrides)
        overridesFolder.copyRecursively(srcDir, overwrite = true)

        logger.info("creating modpack {}", manifest.name)

        val modpack = Modpack(name = manifest.name, version = manifest.version, mcVersion = manifest.minecraft.version)

        val forge = manifest.minecraft.modLoaders.find { it.id.startsWith("forge") }
        if (forge != null) {
            modpack.forge = forge.id.substringAfterLast('.')
            logger.info("set forge version {}", modpack.forge)
        }

        // add curse entries
        manifest.files.forEach { curseFile ->
            logger.info("processing file {}", curseFile)
            val addon = CurseUtil.getAddon(curseFile.projectID)!!
            val file = CurseUtil.getAddonFile(curseFile.projectID, curseFile.fileID)!!
            val entry = Entry(name = addon.name, version = file.fileName, optional = !curseFile.required)
            Entry(name = addon.name, version = file.fileName, optional = !curseFile.required)
            if (!file.gameVersion.contains(modpack.mcVersion)) {
                logger.warn("file {} is for mc versions {}", file.fileName, file.gameVersion)
                file.gameVersion.forEach {
                    if (modpack.mcVersion != it) {
                        if (!entry.validMcVersions.contains(it)) {
                            entry.validMcVersions += it
                        }
                    }
                }
            }
            if(!modpack.mods.releaseTypes.contains(file.releaseType)) {
                entry.releaseTypes = modpack.mods.releaseTypes + file.releaseType
                logger.warn("file {} is release type {}", file.fileName, file.releaseType)
            }
            modpack.mods.entries += entry
        }

        val modsFolder = srcDir.resolve("mods")
        if (modsFolder.exists()) {
            val localFolder = workingDirectory.resolve("local").resolve(manifest.name)
            localFolder.mkdirs()
            modsFolder.copyRecursively(localFolder, overwrite = true)
            modsFolder.deleteRecursively()
            localFolder.walkTopDown().forEach {
                println(it)
                if (it.isFile) {
                    val relativePath = it.toRelativeString(localFolder.parentFile)
                    val targetPath = it.toRelativeString(localFolder)
                    logger.info("processing file {}", targetPath)
                    modpack.mods.entries += Entry(provider = Provider.LOCAL, fileSrc = relativePath, fileName = targetPath)
                }
            }
        }

        //TODO: forge


        logger.info("writing {}", packDef)
        writeToFile(packDef, modpack)
        logger.info("imported {} version: {}", modpack.name, modpack.version)
    }
}