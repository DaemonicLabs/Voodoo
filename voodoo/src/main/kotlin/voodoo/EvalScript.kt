package voodoo

import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.api.resultOrNull
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.JvmScriptCompiler
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.system.exitProcess

fun createJvmScriptingHost(cacheDir: File): BasicJvmScriptingHost {
    val cache = FileBasedScriptCache(cacheDir)
    val compiler = JvmScriptCompiler(defaultJvmScriptingHostConfiguration, cache = cache)
    val evaluator = BasicJvmScriptEvaluator()
    val host = BasicJvmScriptingHost(compiler = compiler, evaluator = evaluator)
    return host
}

inline fun <reified T : Any> BasicJvmScriptingHost.evalScript(
    libs: File,
    scriptFile: File,
    vararg args: Any?,
    compilationConfig: ScriptCompilationConfiguration = createJvmCompilationConfigurationFromTemplate<T> {
        jvm {
            dependenciesFromCurrentContext(wholeClasspath = false)

            if (libs.exists()) {
                libs.walkTopDown()
                    .filter { file ->
                        file.name.endsWith(".jar")
                    }
                    .map {
                        dependencies.append(JvmDependency(it))
                    }
            }
//            val JDK_HOME = System.getProperty("voodoo.jdkHome") ?: System.getenv("JAVA_HOME")
//                ?: throw IllegalStateException("please pass -Dvoodoo.jdkHome=path/to/jdk or please set JAVA_HOME to the installed jdk")
//            jdkHome(File(JDK_HOME))
        }
    }
): T {
    println("compilationConfig entries")
    compilationConfig.entries().forEach {
        println("    $it")
    }

    val evaluationConfig = ScriptEvaluationConfiguration {
        constructorArgs.append(*args)
    }

    println("evaluationConfig entries")
    evaluationConfig.entries().forEach {
        println("    $it")
    }

    val scriptSource = scriptFile.toScriptSource()

    println("compiling script, please be patient")
    val result = eval(scriptSource, compilationConfig, evaluationConfig)

    return result.get<T>(scriptFile)
}

fun SourceCode.Location.posToString() = "(${start.line}, ${start.col})"

inline fun <reified T> ResultWithDiagnostics<EvaluationResult>.get(scriptFile: File): T {

    for (report in reports) {
        println(report)
        val severityIndicator = when (report.severity) {
            ScriptDiagnostic.Severity.FATAL -> "fatal"
            ScriptDiagnostic.Severity.ERROR -> "e"
            ScriptDiagnostic.Severity.WARNING -> "w"
            ScriptDiagnostic.Severity.INFO -> "i"
            ScriptDiagnostic.Severity.DEBUG -> "d"
        }
        println("$severityIndicator: ${report.sourcePath}: ${report.location?.posToString()}: ${report.message}")
        report.exception?.printStackTrace()
    }
    println(this)
    val evalResult = resultOrNull() ?: run {
        Voodoo.logger.error("evaluation failed")
        exitProcess(1)
    }

    val resultValue = evalResult.returnValue
    println("resultValue = '$resultValue'")
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
            Voodoo.logger.error("evaluation failed")
            exitProcess(-1)
        }
    }
}