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
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.toScriptSource

@Repeatable
@Target(AnnotationTarget.FILE)
annotation class Include(val filename: String) {
    companion object {
        fun configureIncludes(
            reports: MutableList<ScriptDiagnostic>,
            context: ScriptConfigurationRefinementContext
        ): ScriptCompilationConfiguration {
            println("collectedData: '${context.collectedData}'")
//            context.collectedData?.entries()?.forEach { (key, value) ->
//                println("collectedData    $key => $value")
//            }
//            context.compilationConfiguration.entries().forEach { (key, value) ->
//                println("compilationConfiguration    $key => $value")
//            }
            val annotations = context.collectedData?.get(ScriptCollectedData.foundAnnotations).also {
                reports += ScriptDiagnostic("found annotations: '$it'", severity = ScriptDiagnostic.Severity.DEBUG)
            }?.takeIf { it.isNotEmpty() }
                ?: return context.compilationConfiguration

            // TODO? make sure rootFolder points at the correct folder

            require(context.script is FileBasedScriptSource) { "${context.script::class} != FileBasedScriptSource" }
            (context.script as? FileBasedScriptSource)?.let { script ->
                SharedFolders.RootDir.value = script.file.parentFile.parentFile
            }

            val includeFolder = SharedFolders.IncludeDir.get()

            require(includeFolder.exists()) { "$includeFolder does not exist" }

            val compilationConfiguration = ScriptCompilationConfiguration(context.compilationConfiguration) {
                annotations.filter { it is Include }.forEach { annotation ->
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
            return compilationConfiguration
        }

        fun configureIncludes(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
            val reports: MutableList<ScriptDiagnostic> = mutableListOf()
            val compilationConfiguration = configureIncludes(reports, context)
            return compilationConfiguration.asSuccess(reports)
        }
    }
}
