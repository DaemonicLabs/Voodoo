package voodoo.pack

import blue.endless.jankson.Jankson
import com.skcraft.launcher.builder.PackageBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import voodoo.data.lock.LockPack
import voodoo.forge.Forge
import voodoo.pack.sk.*
import voodoo.provider.Provider
import voodoo.util.download
import voodoo.util.readJson
import voodoo.util.writeJson
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

object SKPack : AbstractPack() {

    override val label = "SK Packer"

    override suspend fun download(modpack: LockPack, target: String?, clean: Boolean, jankson: Jankson) {
        val cacheDir = directories.cacheHome
        val workspaceDir = modpack.rootFolder.resolve("workspace").absoluteFile
        val modpackDir = workspaceDir.resolve(modpack.id)

        val skSrcFolder = modpackDir.resolve("src")
        logger.info("cleaning modpack directory $skSrcFolder")
        skSrcFolder.deleteRecursively()
        logger.info("copying files into src ${modpack.sourceFolder}")
        val packSrc = modpack.sourceFolder
        if (skSrcFolder.startsWith(packSrc)) {
            throw IllegalStateException("cannot copy parent rootFolder '$packSrc' into subfolder '$skSrcFolder'")
        }
        if (packSrc.exists()) {
            logger.debug("cp -r $packSrc $skSrcFolder")
            packSrc.copyRecursively(skSrcFolder, overwrite = true)
            skSrcFolder.walkBottomUp().forEach {
                if (it.name.endsWith(".entry.hjson") || it.name.endsWith(".lock.json"))
                    it.delete()
                if (it.isDirectory && it.listFiles().isEmpty()) {
                    it.delete()
                }
            }
        } else {
            logger.warn("minecraft directory $packSrc does not exist")
        }

        for (file in skSrcFolder.walkTopDown()) {
            when {
                file.name == "_SERVER" -> file.deleteRecursively()
                file.name == "_CLIENT" -> file.renameTo(file.parentFile)
            }
        }

        val loadersFolder = modpackDir.resolve("loaders")
        logger.info("cleaning loaders $loadersFolder")
        loadersFolder.deleteRecursively()

        val pool = newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors() + 1, "pool")
        val jobs = mutableListOf<Job>()

        // download forge
        val (forgeUrl, forgeFileName, forgeLongVersion, forgeVersion) = Forge.getForgeUrl(modpack.forge.toString(), modpack.mcVersion)
        val forgeFile = loadersFolder.resolve(forgeFileName)
        jobs += launch(context = pool) {
            forgeFile.download(forgeUrl, cacheDir.resolve("FORGE").resolve(forgeVersion))
        }
        val modsFolder = skSrcFolder.resolve("mods")
        logger.info("cleaning mods $modsFolder")
        modsFolder.deleteRecursively()


        val fileChannel = Channel<Pair<String, File>> (Channel.CONFLATED)
        // download entries
        for (entry in modpack.entrySet) {
            jobs += launch(context = pool) {
                val provider = Provider.valueOf(entry.provider).base

                val folder = skSrcFolder.resolve(entry.file).parentFile

                val (url, file) = provider.download(entry, folder, cacheDir)
                if (url != null && entry.useUrlTxt) {
                    val urlTxtFile = folder.resolve(file.name + ".url.txt")
                    urlTxtFile.writeText(url)
                }
                println("done: ${entry.id} $file")
                fileChannel.send(entry.id to file)  // file.relativeTo(skSrcFolder
            }
            logger.info("started job: download '${entry.id}'")
            delay(10)
        }

        delay(10)
        logger.info("waiting for file jobs to finish")
        fileChannel.use { _ ->
            jobs.joinAll()
        }
        val targetFiles = fileChannel.toMap()

        val featureChannel = Channel<SKFeatureComposite>(Channel.CONFLATED)
        // write features
        for (feature in modpack.features) {
            logger.info("processing feature: ${feature.properties.name}")
            jobs += launch(context = pool) {
                for (id in feature.entries) {
                    logger.info(id)

                    val targetFile = targetFiles[id]!!.let {
                        if (it.parentFile.name == "_SERVER" || it.parentFile.name == "_CLIENT") {
                            it.parentFile.parentFile.resolve(it.name)
                        } else
                            it
                    }

                    feature.files.include += targetFile.relativeTo(skSrcFolder).path
                            .replace('\\', '/')
                            .replace("[", "\\[")
                            .replace("]", "\\]")
                    logger.info("includes = ${feature.files.include}")
                }

                if (feature.properties.name.isBlank()) {
//                feature.properties.name = modpack.entries.find {it.id == feature.}
                }

                logger.info("entries: ${feature.entries}")
                logger.info("properties: ${feature.properties}")

                featureChannel.send(
                        SKFeatureComposite(
                                properties = feature.properties,
                                files = feature.files
                        )
                )
                logger.info("processed feature $feature")
            }
            logger.info("started job: feature '${feature.properties.name}'")
            delay(10)
        }

        delay(10)
        logger.info("waiting for feature jobs to finish")

        featureChannel.use { _ ->
            jobs.joinAll()
        }
        val features = featureChannel.toList()

        val skmodpack = SKModpack(
                name = modpack.id,
                title = modpack.title,
                gameVersion = modpack.mcVersion,
                userFiles = modpack.userFiles,
                launch = modpack.launch,
                features = features
        )
        val modpackPath = modpackDir.resolve("modpack.json")
        modpackPath.writeJson(skmodpack)

        // add to workspace.json
        logger.info("adding {} to workpace.json", modpack.id)
        val workspaceMetaFolder = workspaceDir.resolve(".modpacks")
        workspaceMetaFolder.mkdirs()
        val workspacePath = workspaceMetaFolder.resolve("workspace.json")
        val workspace = if (workspacePath.exists()) {
            workspacePath.readJson()
        } else {
            SKWorkspace()
        }
        workspace.packs += SKLocation(modpack.id)

        workspacePath.writeJson(workspace)

        val targetDir = if (target != null) {
            File(target)
        } else {
            workspaceDir.resolve("_upload")
        }

        val manifestDest = targetDir.resolve("${modpack.id}.json")

        val uniqueVersion = "${modpack.version}." + DateTimeFormatter
                .ofPattern("yyyyMMddHHmm")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now())

        PackageBuilder.main(
                arrayOf(
                        "--version", uniqueVersion,
                        "--input", modpackDir.path,
                        "--output", targetDir.path,
                        "--manifest-dest", manifestDest.path
                )
        )

        //regenerate packages.json
        val packagesFile = targetDir.resolve("packages.json")
        val packages: SKPackages = if (packagesFile.exists()) {
            packagesFile.readJson()
        } else {
            SKPackages()
        }

        val packFragment = packages.packages.find { it.name == modpack.id }
                ?: SkPackageFragment(
                        title = modpack.title,
                        name = modpack.id,
                        version = uniqueVersion,
                        location = "${modpack.id}.json"
                ).apply { packages.packages += this }
        packFragment.version = uniqueVersion
        packagesFile.writeJson(packages)
    }
}