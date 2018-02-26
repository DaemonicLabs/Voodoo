package voodoo.importer

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import khttp.get
import mu.KLogging
import mu.KotlinLogging
import voodoo.builder.curse.CurseManifest
import voodoo.builder.curse.CurseUtil
import voodoo.builder.data.Entry
import voodoo.builder.data.Modpack
import voodoo.builder.provider.Provider
import voodoo.util.Directories
import voodoo.util.UnzipUtility
import voodoo.writeToFile
import java.io.File

/**
 * Created by nikky on 30/01/18.
 * @author Nikky
 * @version 1.0
 */

private val logger = KotlinLogging.logger {}

fun main(vararg args: String) = mainBody {

    val arguments = Arguments(ArgParser(args))

    arguments.run {
        val output = workingDir.resolve(outputArg)
        Importer.import(importArg, target, workingDir, output)
    }

}

private class Arguments(parser: ArgParser) {
    val importArg by parser.positional("PACK",
            help = "curse pack file or url")

    val target by parser.storing("--target",
            help = "Modpack definition file")
            .default("")

    val workingDir by parser.storing("-d", "--directory",
            help = "working directory") { File(this) }
            .default(File(System.getProperty("user.dir")))

    val outputArg by parser.storing("-o", "--output",
            help = "output directory")
            .default("modpacks")




//    val verbose by parser.flagging("-v", "--verbose",
//            help = "enable verbose mode")

    //            .addValidator {
//                for(path in value) {
//                    if(path.isAbsolute && !path.exists()) {
//                        throw InvalidArgumentException("$path does not exist")
//                    }
//                }
//            }
}

object Importer : KLogging() {
    fun import(import: String, target: String, workingDirectory: File, outPath: File) {
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

        val packDef = if (target.isNotBlank()) {
            File(target)
        } else workingDirectory.resolve(manifest.name + ".yaml")

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
        for(curseFile in manifest.files) {
            logger.info("processing file {}", curseFile)
            val addon = CurseUtil.getAddon(curseFile.projectID)
            if(addon == null) {
                logger.error("cannot find addon ${curseFile.projectID}")
                continue
            }
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


        logger.info("writing {}", packDef)
        modpack.writeToFile(packDef)
        logger.info("imported {} version: {}", modpack.name, modpack.version)
    }
}