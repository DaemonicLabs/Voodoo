package voodoo

import voodoo.util.SharedFolders
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCollectedData
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.foundAnnotations
import kotlin.script.experimental.api.importScripts
import kotlin.script.experimental.host.toScriptSource

@Repeatable
@Target(AnnotationTarget.FILE)
annotation class Include(val filename: String) {
    companion object {
        fun configureIncludes(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
            val reports: MutableList<ScriptDiagnostic> = mutableListOf()
//            println("collectedData: '${context.collectedData}'")
//            context.collectedData?.entries()?.forEach { (key, value) ->
//                println("collectedData    $key => $value")
//            }
            val annotations = context.collectedData?.get(ScriptCollectedData.foundAnnotations).also {
                //                println("found annotations: '$it'")
                reports += ScriptDiagnostic("found annotations: '$it'", severity = ScriptDiagnostic.Severity.INFO)
            }?.takeIf { it.isNotEmpty() }
                ?: return ScriptCompilationConfiguration(context.compilationConfiguration).asSuccess()

            val includeFolder = SharedFolders.IncludeDir.get()

            val compilationConfiguration = ScriptCompilationConfiguration(context.compilationConfiguration) {
                annotations.forEach { annotation ->
                    val include = annotation as Include
                    val includedScript = includeFolder.resolve(include.filename)
                    require(includedScript.exists()) { "$includedScript does not exist" }
                    importScripts.append(includedScript.toScriptSource())
//                    println("including '$includedScript'")
                    reports += ScriptDiagnostic(
                        "including '$includedScript'",
                        severity = ScriptDiagnostic.Severity.INFO
                    )
                }
            }
//            compilationConfiguration.entries().forEach {
//                println("onAnnotations    $it")
//            }
            return compilationConfiguration.asSuccess(reports)
        }
    }
}
