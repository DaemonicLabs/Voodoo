package voodoo.server

import com.eyeem.watchadoin.Stopwatch
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.data.lock.LockPack
import voodoo.server.installer.GeneratedConstants
import java.io.File

class InstallServerCommand: CliktCommand() {
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    val parentFolder = File(InstallServerCommand::class.java.protectionDomain.codeSource.location.toURI()).parentFile

    val targetDir by argument(
        "TARGET",
        help = "target path for the server install"
    ).file(mustExist = true, canBeFile = false, canBeDir = true)

    val packFile by option(
        "--pack",
        help = "lockpack or folder containing lockpack"
    ).file(mustExist = true, canBeFile = true, canBeDir = true)
        .defaultLazy {
            parentFolder.resolve(LockPack.FILENAME).takeIf { it.exists() }
                ?: File(LockPack.FILENAME)
        }
        .validate { packFile ->
            require(packFile.exists()) { "$packFile does not exist" }
            require(packFile.isFile && packFile.name.endsWith(LockPack.FILENAME)
                    || packFile.isDirectory && packFile.resolve(LockPack.FILENAME).exists()) {
                "$packFile must either be a ${LockPack.FILENAME} file or a directoy containing ${LockPack.FILENAME}"
            }
        }

    val clean by option(
        "--clean",
        help = "clean install (WARNING: will delete server contents before install)"
    ).flag()

    val cleanConfig by option(
        "--cleanConfigs",
        help = "delete all configs before install"
    ).flag()

    val skipModloaderInstall by option(
        "--skipModloaderInstall",
        help = "do not run the modloader installer"
    ).flag()

//        .defaultLazy {
//            parentFolder.resolve("pack.txt").takeIf { it.exists() }?.readText()?.let { path ->
//                parentFolder.resolve(path).path
//            }
//        }

    init {
        versionOption(GeneratedConstants.FULL_VERSION)
    }

    override fun run(): Unit = withLoggingContext("command" to commandName) {
        runBlocking(MDCContext()) {

            val stopwatch = Stopwatch(commandName)
            stopwatch {
                val packFile = when {
                    packFile.exists() && packFile.isFile && packFile.name.endsWith(LockPack.FILENAME) -> packFile
                    packFile.exists() && packFile.isDirectory && packFile.resolve(LockPack.FILENAME).exists() -> packFile.resolve(LockPack.FILENAME)
                    else ->error("cannot find $packFile")
                }

                val modpack = LockPack.parse(
                    packFile = packFile,
                    baseFolder = packFile.absoluteFile.parentFile
                )

                Server.install(
                    stopwatch = "install".watch,
                    modpack = modpack,
                    serverDir = targetDir,
                    skipModloaderInstall = skipModloaderInstall,
                    clean = clean,
                    cleanConfig = cleanConfig
                )
            }

            logger.info { "performance: \n" + stopwatch.toStringPretty() }

        }
    }
}
