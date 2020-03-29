package voodoo

import com.eyeem.watchadoin.Stopwatch
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.JvmScriptCompiler
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.system.exitProcess

fun createJvmScriptingHost(cacheDir: File): BasicJvmScriptingHost {
//    val cache = FileBasedScriptCache(cacheDir)
    val compiler = JvmScriptCompiler(defaultJvmScriptingHostConfiguration) //, cache = cache)
    val evaluator = BasicJvmScriptEvaluator()
    val host = BasicJvmScriptingHost(compiler = compiler, evaluator = evaluator)
    return host
}

inline fun <reified T : Any> BasicJvmScriptingHost.evalScript(
    stopwatch: Stopwatch,
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
): T = stopwatch {
    println("compilationConfig entries $scriptFile ${T::class.simpleName}")
    compilationConfig.entries().forEach { (key, value) ->
        println("    ${key.name}: $value")
    }

    val evaluationConfig = ScriptEvaluationConfiguration {
        constructorArgs.append(*args)
    }

    println("evaluationConfig entries $scriptFile ${T::class.simpleName}")
    evaluationConfig.entries().forEach {
        println("    $it")
    }

    val scriptSource = scriptFile.toScriptSource()

    println("compiling script $scriptFile, please be patient")
    val result = eval(scriptSource, compilationConfig, evaluationConfig)

    end()
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
    val evalResult = valueOr {
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

//            val result = resultValue.value as T
//            println(result)
            resultValue.scriptInstance as T
        }
        is ResultValue.Unit -> {
            Voodoo.logger.info("evaluation returned Unit")
            resultValue.scriptInstance as T
        }
        is ResultValue.Error -> {
            Voodoo.logger.error("evaluation failed with $resultValue")
            exitProcess(-1)
        }
        ResultValue.NotEvaluated -> {
            Voodoo.logger.error("not evaluated")
            exitProcess(-1)
        }
    }
}