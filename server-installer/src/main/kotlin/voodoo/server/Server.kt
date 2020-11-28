package voodoo.server

import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import Modloader
import voodoo.data.Side
import voodoo.data.lock.LockPack
import voodoo.fabric.FabricUtil
import voodoo.forge.ForgeUtil
import voodoo.provider.Providers
import voodoo.util.Directories
import voodoo.util.Downloader.logger
import voodoo.util.download
import voodoo.util.withPool
import java.io.File

/**
 * Created by nikky on 06/05/18.
 * @author Nikky
 */

object Server {
    private val directories = Directories.get(moduleName = "server")

    suspend fun install(
        stopwatch: Stopwatch,
        modpack: LockPack,
        serverDir: File,
        skipModloaderInstall: Boolean,
        clean: Boolean,
        cleanConfig: Boolean
    ) = stopwatch {
        val cacheDir = directories.cacheHome

        if (clean) {
            logger.info("cleaning modpack directory $serverDir")
            serverDir.deleteRecursively()
        }
        serverDir.mkdirs()

        if (cleanConfig) {
            serverDir.resolve("config").deleteRecursively()
        }
        serverDir.resolve("mods").deleteRecursively()

        val srcDir = modpack.sourceFolder
        logger.info("copying files into server dir $srcDir -> $serverDir")
        if (srcDir.exists()) {
            srcDir.copyRecursively(serverDir, overwrite = true)

            serverDir.walkBottomUp().forEach { file ->
                when {
                    file.name.endsWith(".lock.pack.json") -> file.delete()
                    file.isDirectory && file.listFiles()!!.isEmpty() -> file.delete()
                }
            }
        } else {
            logger.warn("minecraft directory $srcDir does not exist")
        }

        for (file in serverDir.walkTopDown()) {
            when {
                file.name == "_CLIENT" -> file.deleteRecursively()
                file.name == "_SERVER" -> {
                    file.copyRecursively(file.absoluteFile.parentFile, overwrite = true)
                    file.deleteRecursively()
                }
            }
        }

        withPool { pool ->
            coroutineScope {
                for (entry in modpack.entries) {
                    if (entry.side == Side.CLIENT) continue
                    launch(context = pool + CoroutineName("job-${entry.id}")) {
                        val provider = Providers[entry.providerType]
                        val targetFolder = serverDir.resolve(entry.path).absoluteFile
                        logger.info("downloading to - ${targetFolder.path}")
                        provider.download(
                            stopwatch = "download-${entry.id}".watch,
                            entry = entry,
                            targetFolder = targetFolder,
                            cacheDir = cacheDir
                        )
                    }
//                delay(10)
                    logger.info("started job ${entry.displayName}")
                }
            }
        }

        when(val modloader = modpack.modloader) {
            is Modloader.Forge -> {
                modloader.forgeVersion
                // download forge
                val (forgeUrl, forgeFileName, forgeLongVersion, forgeVersion) = ForgeUtil.forgeVersionOf(modloader)
                val forgeFile = directories.runtimeDir.resolve(forgeFileName)
                forgeFile.parentFile.mkdirs()
                logger.info("forge: $forgeLongVersion")
                "download-forge".watch {
                    forgeFile.download(forgeUrl, cacheDir.resolve("FORGE").resolve(forgeVersion))
                }

                // install forge
                if (!skipModloaderInstall) {
                    "install-forge".watch {
                        val java = arrayOf(System.getProperty("java.home"), "bin", "java").joinToString(File.separator)
                        val args = arrayOf(java, "-jar", forgeFile.path, "--installServer")
                        logger.debug("running " + args.joinToString(" ") { "\"$it\"" })
                        ProcessBuilder(*args)
                            .directory(serverDir)
                            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                            .redirectError(ProcessBuilder.Redirect.INHERIT)
                            .start()
                            .waitFor()

                        // rename forge jar
//                        val forgeJar = serverDir.resolve("forge-$forgeLongVersion-universal.jar")
//                        val targetForgeJar = serverDir.resolve("forge.jar")
//                        targetForgeJar.delete()
//                        forgeJar.copyTo(targetForgeJar, overwrite = true)
                    }
                } else {
                    val forgeJar = serverDir.resolve("forge-installer.jar")
                    forgeFile.copyTo(forgeJar, overwrite = true)
                }
            }
            is Modloader.Fabric -> {
                val installer = FabricUtil.getInstallers().first { it.version == modloader.installer }
                val installerFile = File(".").resolve("fabric-installer-${modloader.installer}.jar")

                "download-fabric".watch {
                    installerFile.download(
                        url = installer.url,
                        cacheDir = cacheDir.resolve("FABRIC").resolve(modloader.installer)
                    )
                }
                // java -jar fabric-installer-0.5.2.39.jar server -dir ./server -loader version -mcversion $mcversion -downloadMinecraft

                if (!skipModloaderInstall) {
                    "install-fabric".watch {
                        val java = arrayOf(System.getProperty("java.home"), "bin", "java").joinToString(File.separator)
                        val args = arrayOf(java, "-jar", installerFile.path, "server",
                            "-dir", serverDir.absolutePath,
                            "-loader", modloader.loader,
                            "-mcversion", modpack.mcVersion,
                            "-downloadMinecraft"
                        )
                        logger.debug("running: " + args.joinToString(" ") { "\"$it\"" })
                        ProcessBuilder(*args)
                            .directory(serverDir)
                            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                            .redirectError(ProcessBuilder.Redirect.INHERIT)
                            .start()
                            .waitFor()
                    }
                }
            }
        }
        logger.info("finished")
    }
}