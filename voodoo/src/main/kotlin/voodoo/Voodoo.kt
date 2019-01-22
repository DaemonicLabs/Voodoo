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
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.MessageDigest
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.api.importScripts
import kotlin.script.experimental.api.resultOrNull
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.CompiledJvmScriptsCache
import kotlin.script.experimental.jvmhost.JvmScriptCompiler
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.impl.KJvmCompiledScript
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

        val cache = FileBasedScriptCache(cacheDir)
        val compiler = JvmScriptCompiler(defaultJvmScriptingHostConfiguration, cache = cache)
        val evaluator = BasicJvmScriptEvaluator()
        val host = BasicJvmScriptingHost(compiler = compiler, evaluator = evaluator)

        val tomeDir = System.getProperty("voodoo.tomeDir")?.asFile ?: rootDir.resolve("tome")
        val docDir = System.getProperty("voodoo.docDir")?.asFile ?: rootDir.resolve("docs")
        val tomeEnv = initTome(host = host, tomeDir = tomeDir, docDir = docDir)
        logger.debug("tomeEnv: $tomeEnv")

        val config = createJvmCompilationConfigurationFromTemplate<MainScriptEnv> {
            jvm {
                dependenciesFromCurrentContext(wholeClasspath = true)

                importScripts.append(
                    *generatedFiles.map { it.toScriptSource() }.toTypedArray()
                )

                val JDK_HOME = System.getenv("JAVA_HOME")
                    ?: throw IllegalStateException("please set JAVA_HOME to the installed jdk")
                jdkHome(File(JDK_HOME))
            }
//            compilerOptions.append("-jvm-target", "1.8")
            compilerOptions.append("-jvm-target", "1.8")
        }
        println("config entries")
        config.entries().forEach {
            println("    $it")
        }

        val evaluationConfig = ScriptEvaluationConfiguration {
            constructorArgs.append(rootDir, id)
        }

        println("evaluationConfig entries")
        evaluationConfig.entries().forEach {
            println("    $it")
        }

//        val scriptFile = File("packs").resolve(scriptFileName)
        val scriptSource = scriptFile.toScriptSource()

        println("compiling script, please be patient")
        val result = host.eval(scriptSource, config, evaluationConfig)

        val scriptEnv = result.get<MainScriptEnv>(scriptFile)

        val nestedPack = scriptEnv.pack

        val packDir = scriptEnv.rootDir
        val packFileName = "$id.pack.hjson"
//    val packFile = packDir.resolve(packFileName)
        val lockFileName = "$id.lock.pack.hjson"
        val lockFile = packDir.resolve(lockFileName)

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

        val compileConfig = createJvmCompilationConfigurationFromTemplate<TomeScript> {
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

            val evalConfig = ScriptEvaluationConfiguration {
                constructorArgs.append(id)
            }

            val scriptSource = scriptFile.toScriptSource()

            println("compiling script ($scriptFile), please be patient")
            val result = host.eval(scriptSource, compileConfig, evalConfig)

            val tomeScriptEnv = result.get<TomeScript>(scriptFile)

            tomeEnv.add(tomeScriptEnv.fileName, tomeScriptEnv.generateHtml)
        }

        return tomeEnv
    }

    fun <T> ResultWithDiagnostics<EvaluationResult>.get(scriptFile: File): T {
        fun SourceCode.Location.posToString() = "(${start.line}, ${start.col})"

        for (report in reports) {
            println(report)
            val severityIndicator = when (report.severity) {
                ScriptDiagnostic.Severity.FATAL -> "fatal"
                ScriptDiagnostic.Severity.ERROR -> "e"
                ScriptDiagnostic.Severity.WARNING -> "w"
                ScriptDiagnostic.Severity.INFO -> "i"
                ScriptDiagnostic.Severity.DEBUG -> "d"
            }
            println("$severityIndicator: ${scriptFile.absoluteFile}: ${report.location?.posToString()}: ${report.message}")
            report.exception?.printStackTrace()
        }
        println(this)
        val evalResult = resultOrNull() ?: run {
            Voodoo.logger.error("evaluation failed")
            exitProcess(1)
        }

        val resultValue = evalResult.returnValue
        println("resultValue = '${resultValue}'")
        println("resultValue::class = '${resultValue::class}'")

        return when (resultValue) {
            is ResultValue.Value -> {
                println("resultValue.name = '${resultValue.name}'")
                println("resultValue.value = '${resultValue.value}'")
                println("resultValue.type = '${resultValue.type}'")

                println("resultValue.value::class = '${resultValue.value!!::class}'")

                val env = resultValue.value as T
                println(env)
                env
            }
            is ResultValue.Unit -> {
                logger.error("evaluation failed")
                exitProcess(-1)
            }
        }
    }
}

private fun File.readCompiledScript(scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript<*> {
    return inputStream().use { fs ->
        ObjectInputStream(fs).use { os ->
            (os.readObject() as KJvmCompiledScript<*>).apply {
                setCompilationConfiguration(scriptCompilationConfiguration)
            }
        }
    }
}

private fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })

private class FileBasedScriptCache(val baseDir: File) : CompiledJvmScriptsCache {
    internal fun uniqueHash(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): String {
        val digestWrapper = MessageDigest.getInstance("MD5")
        digestWrapper.update(script.text.toByteArray())
        scriptCompilationConfiguration.entries().sortedBy { it.key.name }.forEach {
            digestWrapper.update(it.key.name.toByteArray())
            digestWrapper.update(it.value.toString().toByteArray())
        }
        return digestWrapper.digest().toHexString()
    }

    override fun get(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript<*>? {
        val prefix = if(script is FileScriptSource) {
            "${script.file.name}-"
        } else ""
        val file = File(baseDir, prefix + uniqueHash(script, scriptCompilationConfiguration))
//        val file = File(baseDir, uniqueHash(script, scriptCompilationConfiguration))
        return if (!file.exists()) null else file.readCompiledScript(scriptCompilationConfiguration)
    }

    override fun store(
        compiledScript: CompiledScript<*>,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ) {
        val prefix = if(script is FileScriptSource) {
            "${script.file.name}-"
        } else ""
        val file = File(baseDir, prefix + uniqueHash(script, scriptCompilationConfiguration))
//        val file = File(baseDir, uniqueHash(script, scriptCompilationConfiguration))
        file.outputStream().use { fs ->
            ObjectOutputStream(fs).use { os ->
                os.writeObject(compiledScript)
            }
        }
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
