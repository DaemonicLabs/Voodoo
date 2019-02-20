package voodoo.server

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import voodoo.data.Side
import voodoo.data.lock.LockPack
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
    private val directories = Directories.get("server")

    suspend fun install(
        modpack: LockPack,
        serverDir: File,
        skipForge: Boolean,
        clean: Boolean,
        cleanConfig: Boolean
    ) {
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
                    file.name.endsWith(".entry.hjson") -> file.delete()
                    file.name.endsWith(".lock.hjson") -> file.delete()
                    file.name.endsWith(".lock.pack.hjson") -> file.delete()
                    file.isDirectory && file.listFiles().isEmpty() -> file.delete()
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
                for (entry in modpack.entrySet) {
                    if (entry.side == Side.CLIENT) continue
                    launch(context = pool + CoroutineName("job-${entry.id}")) {
                        val provider = Providers[entry.provider]
                        val targetFolder = serverDir.resolve(entry.serialFile).absoluteFile.parentFile
                        logger.info("downloading to - ${targetFolder.path}")
                        val (_, _) = provider.download(entry, targetFolder, cacheDir)
                    }
//                delay(10)
                    logger.info("started job ${entry.displayName}")
                }
            }
        }

        // download forge
        modpack.forge?.also { forge ->
            val (forgeUrl, forgeFileName, forgeLongVersion, forgeVersion) = ForgeUtil.forgeVersionOf(forge)
            val forgeFile = directories.runtimeDir.resolve(forgeFileName)
            logger.info("forge: $forgeLongVersion")
            forgeFile.download(forgeUrl, cacheDir.resolve("FORGE").resolve(forgeVersion))

            // install forge
            if (!skipForge) {
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
                val forgeJar = serverDir.resolve("forge-$forgeLongVersion-universal.jar")
                val targetForgeJar = serverDir.resolve("forge.jar")
                targetForgeJar.delete()
                forgeJar.copyTo(targetForgeJar, overwrite = true)
            } else {
                val forgeJar = serverDir.resolve("forge-installer.jar")
                forgeFile.copyTo(forgeJar, overwrite = true)
            }
        }

        logger.info("finished")
    }
}