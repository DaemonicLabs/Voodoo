package voodoo

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.runBlocking
import mu.KLogging
import voodoo.builder.Builder
import voodoo.builder.Importer
import voodoo.data.lock.LockPack
import voodoo.script.MainScriptEnv
import voodoo.script.TomeScript
import voodoo.tome.TomeEnv
import voodoo.util.Directories
import voodoo.util.asFile
import voodoo.voodoo.VoodooConstants
import java.io.File
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.system.exitProcess

object Voodoo : KLogging() {
    @JvmStatic
    fun main(vararg fullArgs: String) {

        logger.debug("system.properties: ${System.getProperties()}")

        if (fullArgs.isEmpty()) {
            GradleSetup.main()
            exitProcess(0)
        }

        val directories = Directories.get(moduleName = "script")
        val cacheDir = directories.cacheHome

        val arguments = fullArgs.drop(1)
        val script = fullArgs.getOrNull(0)?.apply {
            require(isNotBlank()) { "configuration script name cannot be blank" }
            require(endsWith(".voodoo.kts")) { "configuration script filename must end with .voodoo.kts" }
        } ?: run {
            logger.error("configuration script must be the first parameter")
            exitProcess(1)
        }
        val scriptFile = File(script)
        require(scriptFile.exists()) { "script file does not exists" }
        val scriptFileName = scriptFile.name

        val id = scriptFileName.substringBeforeLast(".voodoo.kts").apply {
            require(isNotBlank()) { "the script file must contain a id in the filename" }
        }

        val rootDir = (System.getProperty("voodoo.rootDir") ?: System.getProperty("user.dir")).asFile.absoluteFile
        val generatedFilesDir =
            System.getProperty("voodoo.generatedSrc")?.asFile ?: rootDir.resolve(".voodoo").absoluteFile
        val generatedFiles = poet(rootDir = rootDir, generatedSrcDir = generatedFilesDir)

        val host = createJvmScriptingHost(cacheDir)

        val tomeDir = System.getProperty("voodoo.docDir")?.asFile ?: rootDir.resolve("tome")
        val docDir = System.getProperty("voodoo.docDir")?.asFile ?: rootDir.resolve("docs")
        val tomeEnv = initTome(host = host, tomeDir = tomeDir, docDir = docDir)
        logger.debug("tomeEnv: $tomeEnv")

        val scriptEnv = host.evalScript<MainScriptEnv>(
            scriptFile,
            args = *arrayOf(rootDir, id),
            importScripts = generatedFiles.map { it.toScriptSource() }
        )

        val nestedPack = scriptEnv.pack

        val packDir = scriptEnv.rootDir
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

                Tome.generate(modpack, lockPack, tomeEnv)
                logger.info("finished")
            },
            "pack" to { args ->
                val modpack = LockPack.parse(lockFile.absoluteFile, rootDir)
                Pack.pack(modpack, *args)
            },
            "test" to { args ->
                val modpack = LockPack.parse(lockFile.absoluteFile, rootDir)
                TesterForDSL.main(modpack, args = *args)
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

    private fun initTome(
        tomeDir: File,
        docDir: File,
        host: BasicJvmScriptingHost
    ): TomeEnv {
        val tomeEnv = TomeEnv(docDir)

        val tomeScripts = tomeDir.listFiles { file ->
            logger.debug("tome testing: $file")
            file.isFile && file.name.endsWith(".tome.kts")
        }

        val compilationConfig = createJvmCompilationConfigurationFromTemplate<TomeScript> {
            jvm {
                dependenciesFromCurrentContext(wholeClasspath = true)

                val JDK_HOME = System.getenv("JAVA_HOME")
                    ?: throw IllegalStateException("please set JAVA_HOME to the installed jdk")
                jdkHome(File(JDK_HOME))
            }
            compilerOptions.append("-jvm-target", "1.8")
        }

        tomeScripts.forEach { scriptFile ->
            require(scriptFile.exists()) { "script file does not exists" }
            val scriptFileName = scriptFile.name

            val id = scriptFileName.substringBeforeLast(".tome.kts").apply {
                require(isNotBlank()) { "the script file must contain a id in the filename" }
            }

            val tomeScriptEnv = host.evalScript<TomeScript>(
                scriptFile = scriptFile,
                args = *arrayOf(id),
                compilationConfig = compilationConfig
            )

            tomeEnv.add(tomeScriptEnv.fileName, tomeScriptEnv.generateHtml)
        }

        return tomeEnv
    }
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
