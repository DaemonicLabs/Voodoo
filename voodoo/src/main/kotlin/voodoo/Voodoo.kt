package voodoo

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.slf4j.LoggerFactory
import voodoo.builder.Builder
import voodoo.builder.Importer
import voodoo.changelog.ChangelogBuilder
import voodoo.data.lock.LockPack
import voodoo.script.ChangeScript
import voodoo.script.MainScriptEnv
import voodoo.script.TomeScript
import voodoo.tome.TomeEnv
import voodoo.util.Directories
import voodoo.util.SharedFolders
import voodoo.util.asFile
import voodoo.voodoo.VoodooConstants
import java.io.File
import java.io.FileNotFoundException
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.system.exitProcess

object Voodoo : KLogging() {
    @JvmStatic
    fun main(vararg fullArgs: String) {
        val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
        rootLogger.level = Level.DEBUG // TODO: pass as -Dvoodoo.debug=true

        logger.debug("using Voodoo: ${VoodooConstants.FULL_VERSION}")
        logger.debug("full arguments: ${fullArgs.joinToString(",", "[", "]") { it }}")
        logger.debug("system.properties:")
        System.getProperties().forEach { k, v ->
            logger.debug { "  $k = $v" }
        }

        if (fullArgs.isEmpty()) {
            GradleSetup.main()
            exitProcess(0)
        }

        val directories = Directories.get(moduleName = "script")
        val cacheDir = directories.cacheHome

        val arguments = fullArgs.drop(1)
        val script = fullArgs.getOrNull(0)?.apply {
            require(isNotBlank()) { "'$this' configuration script name cannot be blank" }
            require(endsWith(".voodoo.kts")) { "'$this' configuration script filename must end with .voodoo.kts" }
        } ?: run {
            logger.error("configuration script must be the first parameter")
            exitProcess(1)
        }
        val scriptFile = File(script)
        require(scriptFile.exists()) { "script file does not exists" }
        val scriptFileName = scriptFile.name

        val id = scriptFileName.substringBeforeLast(".voodoo.kts").apply {
            require(isNotBlank()) { "the script file must contain a id in the filename" }
        }.toLowerCase()

        logger.debug("id: $id")

        val rootDir = SharedFolders.RootDir.get().absoluteFile

        val host = createJvmScriptingHost(cacheDir)

        val uploadDir = SharedFolders.UploadDir.get(id)

        val libs = rootDir.resolve("libs") // TODO: set from system property
        val tomeDir = System.getProperty("voodoo.tomeDir")?.asFile ?: rootDir.resolve("tome")
        val docDir = /*System.getProperty("voodoo.docDir")?.asFile ?:*/ uploadDir.resolve("docs")
        val tomeEnv = initTome(
            libs = libs, host = host, tomeDir = tomeDir, docDir = docDir)
        logger.debug("tomeEnv: $tomeEnv")
        val changelogBuilder = initChangelogBuilder(
            libs = libs, id = id, tomeDir = tomeDir, host = host)

        val scriptEnv = host.evalScript<MainScriptEnv>(
            libs = libs,
            scriptFile = scriptFile,
            args = *arrayOf(rootDir, id)
        )

        val nestedPack = scriptEnv.pack

        val packFileName = "$id.pack.hjson"
//    val packFile = packDir.resolve(packFileName)
        val lockFileName = "$id.lock.pack.hjson"
        val lockFile = scriptEnv.pack.sourceFolder.resolve(lockFileName)

        val funcs = mapOf<String, suspend (Array<String>) -> Unit>(
            "import_debug" to { _ -> Importer.flatten(nestedPack, targetFileName = packFileName) },
//        "build_debug" to { args -> BuilderForDSL.build(packFile, rootDir, id, targetFileName = lockFileName, args = *args) },
            "build" to { args ->
                val modpack = Importer.flatten(nestedPack)
                val lockPack = Builder.build(modpack, id = id, /*targetFileName = lockFileName,*/ args = *args)

                Tome.generate(modpack, lockPack, tomeEnv, uploadDir)

                try {
                    val diff = Diff.createDiff(
                        docDir = docDir,
                        rootDir = rootDir.absoluteFile,
                        newPack = lockPack,
                        changelogBuilder = changelogBuilder
                    )
                    logger.debug { "diff: $diff" }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }

                logger.info("finished")
            },
            "diff" to { _ ->
                val modpack = LockPack.parse(lockFile.absoluteFile, rootDir)
                val diff = Diff.createDiff(
                    docDir = docDir,
                    rootDir = rootDir.absoluteFile,
                    newPack = modpack,
                    changelogBuilder = changelogBuilder
                )
            },
            "pack" to { args ->
                val modpack = LockPack.parse(lockFile.absoluteFile, rootDir)
                Pack.pack(modpack, uploadDir, *args)
            },
            "test" to { args ->
                val modpack = LockPack.parse(lockFile.absoluteFile, rootDir)
                TesterForDSL.main(modpack, args = *args)
            },
            "release" to { args ->
                // TODO: create a release
                // TODO: create a commit and tag ?
                // TODO: grab latest changelog and append it to full_changelog
            },
            "version" to { _ ->
                logger.info(VoodooConstants.FULL_VERSION)
            }
        )

        fun printCommands(cmd: String?) {
            if (cmd == null) {
                logger.error("no command specified")
            } else {
                logger.error("unknown command '$cmd'")
            }
            logger.warn("voodoo ${VoodooConstants.FULL_VERSION}")
            logger.warn("commands: ")
            funcs.keys.forEach { key ->
                logger.warn("> $key")
            }
        }

        val invocations = arguments.chunkBy(separator = "-")
        invocations.forEach { argChunk ->
            val command = argChunk.getOrNull(0) ?: run {
                printCommands(null)
                return
            }
            logger.info("executing command [${argChunk.joinToString()}]")
            val remainingArgs = argChunk.drop(1).toTypedArray()

            val function = funcs[command.toLowerCase()]
            if (function == null) {
                printCommands(command)
                return
            }

            runBlocking(CoroutineName("main")) {
                function(remainingArgs)
            }
        }
    }

    private fun initChangelogBuilder(
        libs: File,
        id: String,
        tomeDir: File,
        host: BasicJvmScriptingHost
    ): ChangelogBuilder {
        tomeDir.resolve("$id.changelog.kts")
            .also { file -> logger.debug { "trying to load: $file" } }
            .takeIf { it.exists() }?.let { idScript ->
            return host.evalScript<ChangeScript>(
                libs = libs,
                scriptFile = idScript
            ).let { script ->
                script.getBuilderOrNull() ?: throw NotImplementedError("builder was not assigned in $idScript")
            }
        }
        tomeDir.resolve("default.changelog.kts")
            .also { file -> logger.debug { "trying to load: $file" } }
            .takeIf { it.exists() }?.let { defaultScript ->
            return host.evalScript<ChangeScript>(
                libs = libs,
                scriptFile = defaultScript
            ).let { script ->
                script.getBuilderOrNull() ?: throw NotImplementedError("builder was not assigned in $defaultScript")
            }
        }
        logger.debug { "falling back to default changelog builder implementation" }
        return ChangelogBuilder()
    }

    private fun initTome(
        libs: File,
        tomeDir: File,
        docDir: File,
        host: BasicJvmScriptingHost
    ): TomeEnv {
        val tomeEnv = TomeEnv(docDir)

        val tomeScripts = tomeDir.listFiles { file ->
            logger.debug("tome testing: $file")
            file.isFile && file.name.endsWith(".tome.kts")
        }

        tomeScripts.forEach { scriptFile ->
            require(scriptFile.exists()) { "script file does not exists" }
            val scriptFileName = scriptFile.name

            val id = scriptFileName.substringBeforeLast(".tome.kts").apply {
                require(isNotBlank()) { "the script file must contain a id in the filename" }
            }

            val tomeScriptEnv = host.evalScript<TomeScript>(
                libs = libs,
                scriptFile = scriptFile,
                args = *arrayOf(id)
            )

            val generator = tomeScriptEnv.getGeneratorOrNull()
                ?: throw NotImplementedError("generator was not assigned in $scriptFile")

            tomeEnv.add(tomeScriptEnv.filename, generator)
        }

        return tomeEnv
    }

    private fun Iterable<String>.chunkBy(separator: String = "-"): List<Array<String>> {
        val result: MutableList<MutableList<String>> = mutableListOf(mutableListOf())
        this.forEach {
            if (it == separator)
                result += mutableListOf<String>()
            else
                result.last() += it
        }
        return result.map { it.toTypedArray() }
    }
}
