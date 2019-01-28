package voodoo.pack

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import voodoo.data.lock.LockPack
import voodoo.forge.ForgeUtil
import voodoo.mcupdater.FastpackUtil
import voodoo.provider.Providers
import voodoo.util.pool
import java.io.File

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

object MCUFastPack : AbstractPack() {
    override val label = "MCU Pack"

    override suspend fun pack(
        modpack: LockPack,
        output: String?,
        clean: Boolean
    ) {
        val cacheDir = SKPack.directories.cacheHome

        val targetDir = if (output != null) {
            modpack.rootDir.resolve(output)
        } else {
            modpack.rootDir.resolve(".fastpack").resolve("_upload")
        }.absoluteFile

        if(clean) {
            targetDir.deleteRecursively()
        }
//
        val workingDir = targetDir.resolve(modpack.id).absoluteFile

        modpack.sourceFolder.let { packSrc ->
            if (packSrc.exists()) {
                SKPack.logger.debug("cp -r $packSrc $workingDir")
                packSrc.copyRecursively(workingDir, overwrite = true)
                workingDir.walkBottomUp().forEach {
                    if (it.name.endsWith(".entry.hjson") || it.name.endsWith(".lock.hjson") || it.name.endsWith(".lock.pack.hjson"))
                        it.delete()
                    if (it.isDirectory && it.listFiles().isEmpty()) {
                        it.delete()
                    }
                }
            } else {
                SKPack.logger.warn("minecraft directory $packSrc does not exist")
            }
        }


        coroutineScope {
//            // download entries
            val deferredFiles: List<Deferred<Pair<String, File>>> = modpack.entrySet.map { entry ->
                async(context = pool + CoroutineName("download-${entry.id}")) {
                    val provider = Providers[entry.provider]

                    val targetFolder = workingDir.resolve(entry.serialFile).parentFile

                    val (url, file) = provider.download(entry, targetFolder, cacheDir)

                    // TODO: wait for fastpack to support
//                    if (url != null && entry.useUrlTxt) {
//                        val urlTxtFile = targetFolder.resolve(file.name + ".url.txt")
//                        urlTxtFile.writeText(url)
//                    }

                    //                println("done: ${entry.id} $file")
                    entry.id to file // serialFile.relativeTo(skSrcFolder
                }.also {
                    SKPack.logger.info("started job: download '${entry.id}'")
                    delay(10)
                }
            }

            delay(10)
            SKPack.logger.info("waiting for file jobs to finish")

            val targetFiles = deferredFiles.awaitAll().toMap()
            logger.debug("targetFiles: $targetFiles")

            // write features
            modpack.features.forEach { feature ->
                launch(pool + CoroutineName("properties-${feature.feature.name}")) {
                    logger.info("processing feature.name: ${feature.feature.name}")
                    logger.info("properties: ${feature.feature}")
                    for (id in feature.entries) {
                        logger.info("id: $id")
                        logger.info("targetfiles: $targetFiles")

                        targetFiles[id]?.let { targetFile ->
                            if(targetFile.parentFile.name != "optional") {
                                val rel = targetFile.relativeTo(workingDir)
                                val target = workingDir.resolve("optional").resolve(rel.parentFile)
                                logger.info("mv $rel -> ${target.relativeTo(workingDir)}")
                                target.parentFile.mkdirs()
                                targetFile.renameTo(target)
                            }
                        }

//                        feature.files.include += targetFile.relativeTo(workingDir).path
//                            .replace('\\', '/')
//                            .replace("[", "\\[")
//                            .replace("]", "\\]")
//                        logger.info("includes = ${feature.files.include}")
                    }

                    logger.info("entries: ${feature.entries}")
                    logger.info("properties: ${feature.feature}")

                    logger.info("processed properties $feature")
                }
            }

            delay(10)
            logger.info("waiting for properties jobs to finish")
        }

        for (file in workingDir.walkBottomUp()) {
            val rel = file.relativeTo(workingDir)
            logger.info("walking: $rel")
            when {
                file.name == "_SERVER" -> {
                    val target = workingDir.resolve("server").resolve(rel.parentFile)
                    logger.info("mv $rel -> ${target.relativeTo(workingDir)}")
                    target.parentFile.mkdirs()
                    file.renameTo(target)
                }
                file.name == "_CLIENT" -> {
                    val target = workingDir.resolve("client").resolve(rel.parentFile)
                    logger.info("mv $rel -> ${target.relativeTo(workingDir)}")
                    target.parentFile.mkdirs()
                    file.renameTo(target)
                }
            }
        }

        val forgeVersion = modpack.forge?.let { forge ->
            val (forgeUrl, forgeFileName, longVersion, forgeVersion) = ForgeUtil.forgeVersionOf(forge)
            forgeVersion
        } ?: run {
            logger.warn { "no forge configured" }
            null
        }

        // TODO: package resourcepacks and other ignored folders into a zip in extract/

        val baseUrl = "https://nikky.moe/files"

        val outFile = targetDir.resolve("${modpack.id}.xml")
        FastpackUtil.execute(
            workingDir, baseUrl, modpack.mcVersion, outFile,
            forge = forgeVersion
        )

        logger.info("upload $targetDir, so that '$outFile' is accessible as ${baseUrl + "/" + outFile.relativeTo(targetDir)}")
    }
}