package voodoo

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.eyeem.watchadoin.Stopwatch
import com.eyeem.watchadoin.TraceEventsReport
import com.eyeem.watchadoin.saveAsSvg
import com.eyeem.watchadoin.asTraceEventsReport
import com.eyeem.watchadoin.saveAsHtml
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
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
        logger.debug("full arguments: ${fullArgs.joinToString(", ", "[", "]") { it }}")
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

        if(!SharedFolders.RootDir.defaultInitialized) {
            SharedFolders.RootDir.default = File(System.getProperty("user.dir")).absoluteFile
        }
        val rootDir = SharedFolders.RootDir.get().absoluteFile

        val host = createJvmScriptingHost(cacheDir)

        val uploadDir = SharedFolders.UploadDir.get(id)

        val stopwatch = Stopwatch("main")

        val reportName = stopwatch {

            val libs = rootDir.resolve("libs") // TODO: set from system property
            val tomeDir = SharedFolders.TomeDir.get()
            val docDir = SharedFolders.DocDir.get(id)
            val tomeEnv = initTome(
                stopwatch = "initTome".watch, libs = libs, host = host, tomeDir = tomeDir, docDir = docDir
            )
            logger.debug("tomeEnv: $tomeEnv")
            val changelogBuilder = initChangelogBuilder(
                stopwatch = "initChangelogBuilder".watch, libs = libs, id = id, tomeDir = tomeDir, host = host
            )

            val scriptEnv = host.evalScript<MainScriptEnv>(
                stopwatch = "evalScript".watch,
                libs = libs,
                scriptFile = scriptFile,
                args = *arrayOf(rootDir, id)
            )

            val nestedPack = scriptEnv.pack

            val packFileName = "$id.pack.hjson"
//    val packFile = packDir.resolve(packFileName)
            val lockFileName = "$id.lock.pack.hjson"
            val lockFile = scriptEnv.pack.sourceFolder.resolve(lockFileName)

            val funcs = mapOf<String, suspend Stopwatch.(Array<String>) -> Unit>(
                "import_debug" to { _ ->
                    Importer.flatten(nestedPack, targetFileName = packFileName)
                },
//        "build_debug" to { args -> BuilderForDSL.build(packFile, rootDir, id, targetFileName = lockFileName, args = *args) },
                "build" to { args ->
                    val modpack = "flatten".watch {
                        Importer.flatten(nestedPack)
                    }
                    val lockPack = "build".watch {
                        Builder.build(this, modpack, id = id, /*targetFileName = lockFileName,*/ args = *args)
                    }
                    "tome".watch {
                        Tome.generate(this, modpack, lockPack, tomeEnv, uploadDir)
                    }

                    // TODO: add changelog field to pack
                    // TODO: write `.meta/$id/$version.meta.hjson`
                    // TODO: if the file did not exist (so it is the first commit that changes the version):
                    //   TODO: get parent git hash, write to `$lastVersion.commithash.txt`
                    // TODO: generate diff between $version and $lastVersion
                    // TODO: generate full changelog between each version in order 0->1, 1->2, lastVersion->version

                    "diff".watch {
                        try {
                            val diff = Diff.createDiff(
                                stopwatch = "createDiff".watch,
                                docDir = docDir,
                                rootDir = rootDir.absoluteFile,
                                newPack = lockPack,
                                changelogBuilder = changelogBuilder
                            )
                            logger.debug { "diff: $diff" }
                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                        }
                    }

                    logger.info("finished")
                },
                "diff" to { _ ->
                    val modpack = LockPack.parse(lockFile.absoluteFile, rootDir)
                    val diff = Diff.createDiff(
                        stopwatch = "createDiff".watch,
                        docDir = docDir,
                        rootDir = rootDir.absoluteFile,
                        newPack = modpack,
                        changelogBuilder = changelogBuilder
                    )
                },
                "pack" to { args ->
                    val modpack = LockPack.parse(lockFile.absoluteFile, rootDir)
                    Pack.pack("pack".watch, modpack, uploadDir, *args)
                },
                "test" to { args ->
                    val modpack = LockPack.parse(lockFile.absoluteFile, rootDir)
                    TesterForDSL.main("test".watch, modpack, args = *args)
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
                val remainingArgs = argChunk.drop(1).toTypedArray()
                logger.info("executing command '$command' with args [${remainingArgs.joinToString()}]")

                val function = funcs[command.toLowerCase()]
                if (function == null) {
                    printCommands(command)
                    return
                }

                runBlocking(CoroutineName("main")) {
                    "${command}Watch".watch {
                        function(remainingArgs)
                    }
                }
            }
            invocations.joinToString("_") { it.joinToString("-") }
        }
        println(stopwatch.toStringPretty())
        val reportDir= rootDir.resolve("reports").apply { mkdirs() }
        stopwatch.saveAsSvg(reportDir.resolve("${id}_$reportName.report.svg"))
        stopwatch.saveAsHtml(reportDir.resolve("${id}_$reportName.report.html"))
        val traceEventsReport = stopwatch.asTraceEventsReport()
        val jsonString = Json(JsonConfiguration(prettyPrint = true, encodeDefaults = true))
            .stringify(TraceEventsReport.serializer(), traceEventsReport)
        reportDir.resolve("${id}_$reportName.report.json").writeText(jsonString)
    }

    private fun initChangelogBuilder(
        stopwatch: Stopwatch,
        libs: File,
        id: String,
        tomeDir: File,
        host: BasicJvmScriptingHost
    ): ChangelogBuilder = stopwatch {
        tomeDir.resolve("$id.changelog.kts")
            .also { file -> logger.debug { "trying to load: $file" } }
            .takeIf { it.exists() }?.let { idScript ->
                return@stopwatch host.evalScript<ChangeScript>(
                    "evalScript_file".watch,
                    libs = libs,
                    scriptFile = idScript
                ).let { script ->
                    script.getBuilderOrNull() ?: throw NotImplementedError("builder was not assigned in $idScript")
                }
            }
        tomeDir.resolve("default.changelog.kts")
            .also { file -> logger.debug { "trying to load: $file" } }
            .takeIf { it.exists() }?.let { defaultScript ->
                return@stopwatch host.evalScript<ChangeScript>(
                    "evalScript_default".watch,
                    libs = libs,
                    scriptFile = defaultScript
                ).let { script ->
                    script.getBuilderOrNull() ?: throw NotImplementedError("builder was not assigned in $defaultScript")
                }
            }
        logger.debug { "falling back to default changelog builder implementation" }
        return@stopwatch ChangelogBuilder()
    }

    private fun initTome(
        stopwatch: Stopwatch,
        libs: File,
        tomeDir: File,
        docDir: File,
        host: BasicJvmScriptingHost
    ): TomeEnv = stopwatch {
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
                "evalScript_tome".watch,
                libs = libs,
                scriptFile = scriptFile,
                args = *arrayOf(id)
            )

            val generator = tomeScriptEnv.getGeneratorOrNull()
                ?: throw NotImplementedError("generator was not assigned in $scriptFile")

            tomeEnv.add(tomeScriptEnv.filename, generator)
        }

        return@stopwatch tomeEnv
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
