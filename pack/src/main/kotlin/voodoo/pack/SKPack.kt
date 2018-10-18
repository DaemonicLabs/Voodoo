package voodoo.pack

import com.skcraft.launcher.builder.FeaturePattern
import com.skcraft.launcher.builder.PackageBuilder
import com.skcraft.launcher.model.SKModpack
import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.awaitAll
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.delay
import kotlinx.serialization.json.JSON
import voodoo.data.lock.LockPack
import voodoo.forge.ForgeUtil
import voodoo.pack.sk.SKLocation
import voodoo.pack.sk.SKPackages
import voodoo.pack.sk.SKWorkspace
import voodoo.pack.sk.SkPackageFragment
import voodoo.provider.Providers
import voodoo.util.download
import voodoo.util.pool
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

object SKPack : AbstractPack() {

    override val label = "SK Packer"

    override suspend fun pack(
            modpack: LockPack,
            target: String?,
            clean: Boolean
    ) {
        val cacheDir = directories.cacheHome
        val workspaceDir = modpack.rootDir.resolve("workspace").absoluteFile
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
                if (it.name.endsWith(".entry.hjson") || it.name.endsWith(".lock.hjson"))
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

        coroutineScope {
            // download forge
            modpack.forge?.also { forge ->
                val (forgeUrl, forgeFileName, _, forgeVersion) = ForgeUtil.forgeVersionOf(forge)
                val forgeFile = loadersFolder.resolve(forgeFileName)
                forgeFile.download(forgeUrl, cacheDir.resolve("FORGE").resolve(forgeVersion))
            } ?: logger.warn { "no forge configured" }
            val modsFolder = skSrcFolder.resolve("mods")
            logger.info("cleaning mods $modsFolder")
            modsFolder.deleteRecursively()

            // download entries
            val deferredFiles: List<Deferred<Pair<String, File>>> = modpack.entrySet.map { entry ->
                async(context = pool + CoroutineName("download-${entry.id}")) {
                    val provider = Providers[entry.provider]

                    val targetFolder = skSrcFolder.resolve(entry.serialFile).parentFile

                    val (url, file) = provider.download(entry, targetFolder, cacheDir)
                    if (url != null && entry.useUrlTxt) {
                        val urlTxtFile = targetFolder.resolve(file.name + ".url.txt")
                        urlTxtFile.writeText(url)
                    }
                    //                println("done: ${entry.id} $file")
                    entry.id to file // serialFile.relativeTo(skSrcFolder
                }.also {
                    logger.info("started job: download '${entry.id}'")
                    delay(10)
                }
            }

            delay(10)
            logger.info("waiting for file jobs to finish")

            val targetFiles = deferredFiles.awaitAll().toMap()
//            logger.debug("targetFiles: $targetFiles")

            // write features
            val deferredFeatures = modpack.features.map { feature ->
                async(pool + CoroutineName("properties-${feature.feature.name}")) {
                    logger.info("processing properties: ${feature.feature.name}")
                    for (id in feature.entries) {
                        logger.info(id)

                        val targetFile = targetFiles[id]?.let { targetFile ->
                            targetFile.parentFile.let { parent ->
                                if (parent.name == "_SERVER" || parent.name == "_CLIENT") {
                                    parent.parentFile.resolve(targetFile.name)
                                } else
                                    targetFile
                            }
                        }!!

                        feature.files.include += targetFile.relativeTo(skSrcFolder).path
                                .replace('\\', '/')
                                .replace("[", "\\[")
                                .replace("]", "\\]")
                        logger.info("includes = ${feature.files.include}")
                    }

                    logger.info("entries: ${feature.entries}")
                    logger.info("properties: ${feature.feature}")

                    logger.info("processed properties $feature")

                    FeaturePattern(
                            feature = feature.feature,
                            filePatterns = feature.files
                    )
                }
            }

            delay(10)
            logger.info("waiting for properties jobs to finish")

            val features = deferredFeatures.awaitAll()

            val skmodpack = SKModpack(
                    name = modpack.id,
                    title = modpack.title,
                    gameVersion = modpack.mcVersion,
                    userFiles = modpack.userFiles,
                    launch = modpack.launch,
                    features = features
            )

            val modpackPath = modpackDir.resolve("modpack.json")
            modpackPath.writeText(JSON.indented.stringify(skmodpack))

            // add to workspace.json
            logger.info("adding ${modpack.id} to workpace.json", modpack.id)
            val workspaceMetaFolder = workspaceDir.resolve(".modpacks")
            workspaceMetaFolder.mkdirs()
            val workspacePath = workspaceMetaFolder.resolve("workspace.json")
            val workspace = if (workspacePath.exists()) {
                try {
                    JSON.indented.parse<SKWorkspace>(workspacePath.readText())
                } catch (e: Exception) {
                    logger.error("failed parsing: $workspacePath", e)
                    SKWorkspace()
                }
            } else {
                SKWorkspace()
            }
            workspace.packs += SKLocation(modpack.id)

            workspacePath.writeText(JSON.indented.stringify(workspace))

            val targetDir = if (target != null) {
                modpack.rootDir.resolve(target)
            } else {
                workspaceDir.resolve("_upload")
            }

            val manifestDest = targetDir.resolve("${modpack.id}.json")

            val uniqueVersion = "${modpack.version}." + DateTimeFormatter
                    .ofPattern("yyyyMMddHHmm")
                    .withZone(ZoneOffset.UTC)
                    .format(Instant.now())

            PackageBuilder.main(
                    "--version", uniqueVersion,
                    "--input", modpackDir.path,
                    "--output", targetDir.path,
                    "--manifest-dest", manifestDest.path,
                    "--pretty-print"
            )

            // regenerate packages.json
            val packagesFile = targetDir.resolve("packages.json")
            val packages: SKPackages = if (packagesFile.exists()) {
                JSON.indented.parse(packagesFile.readText())
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
            packagesFile.writeText(JSON.indented.stringify(packages))

            logger.info("finished")
        }
    }
}