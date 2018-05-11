package voodoo.server

import voodoo.data.Side
import voodoo.data.lock.LockPack
import voodoo.forge.Forge
import voodoo.provider.Provider
import voodoo.util.Directories
import voodoo.util.download
import voodoo.util.downloader.logger
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Created by nikky on 06/05/18.
 * @author Nikky
 * @version 1.0
 */

object Server {
    val directories = Directories.get(moduleName = "server-installer")

    fun install(modpack: LockPack, serverDir: File, clean: Boolean) {
        val cacheDir = directories.cacheHome

        if (clean) {
            logger.info("cleaning modpack directory $serverDir")
            serverDir.deleteRecursively()
        }
        serverDir.mkdirs()

        serverDir.resolve("config").deleteRecursively()
        serverDir.resolve("mods").deleteRecursively()

        logger.info("copying files into server dir")
        val mcDir = File(modpack.minecraftDir)
        if (mcDir.exists()) {
            mcDir.copyRecursively(serverDir, overwrite = true) //TODOO filter _CLIENT and strip _SERVER from paths
        } else {
            logger.warn("minecraft directory $mcDir does not exist")
        }

        for(file in serverDir.walkTopDown()) {
            when {
                file.name == "_CLIENT" -> file.deleteRecursively()
                file.name == "_SERVER" -> file.renameTo(file.parentFile)
            }
        }

        // download forge
        val (forgeUrl, forgeFileName, forgeLongVersion, forgeVersion) = Forge.getForgeUrl(modpack.forge.toString(), modpack.mcVersion)
        val forgeFile = directories.runtimeDir.resolve(forgeFileName)
        forgeFile.download(forgeUrl, cacheDir.resolve("FORGE").resolve(forgeVersion))

        logger.info("forge: $forgeLongVersion")

        // install forge
        val java = arrayOf(System.getProperty("java.home"), "bin", "java").joinToString(File.separator)
        val args = arrayOf(java, "-jar", forgeFile.path, "--installServer")
        logger.debug("running " + args.joinToString(" ") { "\"$it\"" })
        ProcessBuilder(*args)
                .directory(serverDir)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
                .waitFor(60, TimeUnit.MINUTES)

        //rename forge jar
        val forgeJar = serverDir.resolve("forge-$forgeLongVersion-universal.jar")
        val targetForgeJar = serverDir.resolve("forge.jar")
        targetForgeJar.delete()
        forgeJar.renameTo(targetForgeJar)

        // download entries
        for (entry in modpack.entries) {
            if(entry.side == Side.CLIENT) continue
            val provider = Provider.valueOf(entry.provider).base
            val targetFolder = serverDir.resolve(entry.folder)
            val (url, file) = provider.download(entry, modpack, targetFolder, cacheDir)
        }

    }


}