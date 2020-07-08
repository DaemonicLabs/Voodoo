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
import com.xenomachina.argparser.ArgParser
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule
import mu.KLogging
import org.slf4j.LoggerFactory
import voodoo.builder.Builder
import voodoo.changelog.ChangelogBuilder
import voodoo.data.ModloaderPattern
import voodoo.data.nested.NestedPack
import voodoo.script.ChangeScript
import voodoo.script.MainScriptEnv
import voodoo.script.TomeScript
import voodoo.tome.TomeEnv
import voodoo.util.Directories
import voodoo.util.SharedFolders
import voodoo.util.json
import voodoo.util.jsonConfiguration
import voodoo.voodoo.main.GeneratedConstants
import java.io.File
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.system.exitProcess

object VoodooMain : KLogging() {
    @JvmStatic
    fun main(vararg fullArgs: String) {
        val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
        rootLogger.level = Level.DEBUG // TODO: pass as -Dvoodoo.debug=true ?
        System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_ON)

        Thread.sleep(1000) // wait for logger to catch up

        logger.debug("using Voodoo: ${GeneratedConstants.FULL_VERSION}")
        logger.debug("full arguments: ${fullArgs.joinToString(", ", "[", "]") { it }}")
//        logger.debug("system.properties:")
//        System.getProperties().forEach { k, v ->
//            logger.debug { "  $k = $v" }
//        }

        if (fullArgs.isEmpty()) {
            GradleSetup.main()
            exitProcess(0)
        }

        val directories = Directories.get(moduleName = "script")
        val cacheDir = directories.cacheHome

        val arguments = fullArgs.drop(1)
        logger.info { "arguments: ${arguments}"}
        val script = fullArgs.getOrNull(0)?.apply {
            require(isNotBlank()) { "'$this' configuration script name cannot be blank" }
            require(endsWith(".voodoo.kts")) { "'$this' configuration script filename must end with .voodoo.kts" }
        } ?: run {
            logger.error("configuration script must be the first parameter")
            exitProcess(1)
        }
        val scriptFile = File(script)
        require(scriptFile.exists()) { "script file does not exists" }

        val id = scriptFile.name.substringBeforeLast(".voodoo.kts").apply {
            require(isNotBlank()) { "the script file must contain a id in the filename" }
        }.toLowerCase()

        logger.debug("id: $id")

        if(!SharedFolders.RootDir.defaultInitialized) {
            SharedFolders.RootDir.value = File(System.getProperty("user.dir")).absoluteFile
        }
        val rootDir = SharedFolders.RootDir.get().absoluteFile


        val uploadDir = SharedFolders.UploadDir.get(id)

        val stopwatch = Stopwatch("main")

        val reportName = stopwatch {
            val host = "createJvmScriptingHost".watch {
                createJvmScriptingHost(cacheDir)
            }

            val libs = rootDir.resolve("libs") // TODO: set from system property
            val tomeDir = SharedFolders.TomeDir.get()
            val docDir = SharedFolders.DocDir.get(id)

            val lockFileName = "$id.lock.pack.json"
            val lockFile = rootDir.resolve(id).resolve(lockFileName)

            logger.info { "fullArgs: ${fullArgs.joinToString()}"}
            logger.info { "arguments: ${arguments}"}
            val funcs = mapOf<String, suspend Stopwatch.(Array<String>) -> Unit>(
                VoodooTask.Build.key to { args ->
                    // TODO: only compile in this step
                    val scriptEnv = host.evalScript<MainScriptEnv>(
                        stopwatch = "evalScript".watch,
                        libs = libs,
                        scriptFile = scriptFile,
                        args = *arrayOf(rootDir, id)
                    )

                    val tomeEnv = initTome(
                        stopwatch = "initTome".watch, libs = libs, host = host, tomeDir = tomeDir, docDir = docDir
                    )
                    logger.debug("tomeEnv: $tomeEnv")

                    val nestedPack = scriptEnv.pack

                    // debug
//                    rootDir.resolve(id).resolve("$id.nested.pack.json").writeText(
//                        json.stringify(NestedPack.serializer(), nestedPack)
//                    )

                    Builder.logger.debug("parsing args: ${args.joinToString(", ")}")
                    val parser = ArgParser(args)
                    val arguments = voodoo.builder.BuilderArguments(parser)
                    parser.force()

                    // TODO: pass extra args object
                    VoodooTask.Build.execute(
                        this,
                        id,
                        nestedPack,
                        tomeEnv,
                        arguments.noUpdate,
                        arguments.entries
                    )
                    logger.info("finished")
                },
                // TODO: git tag task
                // TODO: make changelog tasks
                VoodooTask.Changelog.key to { _ ->
                    val changelogBuilder = initChangelogBuilder(
                        stopwatch = "initChangelogBuilder".watch, libs = libs, id = id, tomeDir = tomeDir, host = host
                    )
                    val tomeEnv = initTome(
                        stopwatch = "initTome".watch, libs = libs, host = host, tomeDir = tomeDir, docDir = docDir
                    )

                    VoodooTask.Changelog.execute(this, id, changelogBuilder, tomeEnv)
                },
                VoodooTask.Pack.key to { args ->
                    // TODO: pass pack method
                    val arguments = voodoo.pack.PackArguments(ArgParser(args))
                    val packer = Pack.packMap[arguments.method.toLowerCase()] ?: run {
                        Pack.logger.error("no such packing method: ${arguments.method}")
                        exitProcess(-1)
                    }
                    VoodooTask.Pack.execute(this, id, packer)
                },
                VoodooTask.Test.key to { args ->
                    // TODO: pass test method
                    val arguments = voodoo.test.TestArguments(ArgParser(args))

                    val testMethod = when (arguments.method) {
                        "mmc" -> TestMethod.MultiMC(clean = arguments.clean)
                        else -> error("no such method found for ${arguments.method}")
                    }

                    VoodooTask.Test.execute(this, id, testMethod)
                },
                VoodooTask.Version.key to { _ ->
                    logger.info("voodoo-main: " + GeneratedConstants.FULL_VERSION)
                    logger.info("voodoo: " + VoodooTask.Version.version)

                    val dependencies = VoodooTask.Version.dependencies()
                    val width = dependencies.keys.map{it.length}.max() ?: 0
                    dependencies.forEach { (project, version) ->
                        logger.info("  ${project}:  ${version.padStart(width)}")
                    }
                }
            )

            fun printCommands(cmd: String?) {
                if (cmd == null) {
                    logger.error("no command specified")
                } else {
                    logger.error("unknown command '$cmd'")
                }
                logger.warn("voodoo ${GeneratedConstants.FULL_VERSION}")
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
        }!!

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
