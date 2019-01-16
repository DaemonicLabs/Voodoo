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
import voodoo.voodoo.VoodooConstants
import java.io.File
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.api.importScripts
import kotlin.script.experimental.api.resultOrNull
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

        val arguments = fullArgs.drop(1)
        val script = fullArgs.getOrNull(0) ?: run {
            logger.error("configuration script must be the first parameter")
            exitProcess(1)
        }


        val rootDir = File(".").absoluteFile
        val generatedFiles = poet(rootDir = rootDir)

//        voodooDir.listFiles().forEach { file ->
//            val evalu = ScriptEvaluationConfiguration {
//
//            }
//            val config = createJvmCompilationConfigurationFromTemplate<ConstScript> {
//                compilerOptions.append("-Jvm-Target 1.8")
//                jvm {
//                    dependenciesFromCurrentContext(wholeClasspath = true)
// //                dependencies.append(JvmDependency(voodooDir))
//                    javaHome(File("/usr/lib/jvm/intellij-jdk")) // TODO use environment variable
//                }
//            }
//            val result = BasicJvmScriptingHost().eval(file.readText().toScriptSource(), config, evalu)
//            println(result)
//            val evalResult = result.resultOrNull() ?: run {
//                logger.error("evaluation failed")
//                exitProcess(1)
//            }
//            println(evalResult)
//            println(evalResult.returnValue)
//            println(evalResult.returnValue.javaClass)
//        }

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
            constructorArgs.append(rootDir)
        }

        println("evaluationConfig entries")
        evaluationConfig.entries().forEach {
            println("    $it")
        }

        val scriptFile = File("packs").resolve(script)
        val absoluteScriptFile = scriptFile.absoluteFile
        val scriptContent = scriptFile.readText()
        val scriptSource = scriptContent.toScriptSource()
        val result = BasicJvmScriptingHost().eval(scriptSource, config, evaluationConfig)

        fun SourceCode.Location.posToString() = "(${start.line}, ${start.col})"

        for (report in result.reports) {
            println(report)
            val severityIndicator = when (report.severity) {
                ScriptDiagnostic.Severity.FATAL -> "fatal"
                ScriptDiagnostic.Severity.ERROR -> "e"
                ScriptDiagnostic.Severity.WARNING -> "w"
                ScriptDiagnostic.Severity.INFO -> "i"
                ScriptDiagnostic.Severity.DEBUG -> "d"
            }
            println("$severityIndicator: $absoluteScriptFile: ${report.location?.posToString()}: ${report.message}")
            report.exception?.printStackTrace()
        }
        println(result)
        val evalResult = result.resultOrNull() ?: run {
            logger.error("evaluation failed")
            exitProcess(1)
        }


        val resultValue = evalResult.returnValue
        println("resultValue = '${resultValue}'")
        println("resultValue::class = '${resultValue::class}'")

        when (resultValue) {
            is ResultValue.Value -> {
                println("resultValue.name = '${resultValue.name}'")
                println("resultValue.value = '${resultValue.value}'")
                println("resultValue.type = '${resultValue.type}'")

                println("resultValue.value::class = '${resultValue.value!!::class}'")


                val env = resultValue.value as MainScriptEnv
                println(env)
                println(env.rootDir)
            }
        }
        exitProcess(0)


        val mainEnv = MainScriptEnv(rootDir = rootDir)

        val nestedPack = mainEnv.nestedPack("abc", "1.12.2") {}

        val packDir = mainEnv.rootDir
        val id = nestedPack.id
        val packFileName = "$id.pack.hjson"
//    val packFile = packDir.resolve(packFileName)
        val lockFileName = "$id.lock.hjson"
        val lockFile = packDir.resolve(lockFileName)

        val funcs = mapOf<String, suspend (Array<String>) -> Unit>(
            "import_debug" to { _ -> Importer.flatten(nestedPack, targetFileName = packFileName) },
//        "build_debug" to { args -> BuilderForDSL.build(packFile, rootDir, id, targetFileName = lockFileName, args = *args) },
            "build" to { args ->
                val modpack = Importer.flatten(nestedPack)
                val lockPack = Builder.build(modpack, name = id, /*targetFileName = lockFileName,*/ args = *args)
                Tome.generate(modpack, lockPack, mainEnv.tomeEnv)
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
//        "idea" to Idea::main, //TODO: generate gradle/idea project ?
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
            voodoo.logger.info("executing command [${argChunk.joinToString()}]")
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
