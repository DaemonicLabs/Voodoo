package voodoo

import voodoo.util.asFile
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCollectedData
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.foundAnnotations
import kotlin.script.experimental.api.importScripts
import kotlin.script.experimental.host.toScriptSource

annotation class Include(val filename: String) {
    companion object {
        fun configureIncludes(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
            val reports: MutableList<ScriptDiagnostic> = mutableListOf()

            val annotations = context.collectedData?.get(ScriptCollectedData.foundAnnotations)?.takeIf { it.isNotEmpty() }
                ?: return ScriptCompilationConfiguration(context.compilationConfiguration).asSuccess()

            val rootDir = (System.getProperty("voodoo.rootDir") ?: System.getProperty("user.dir")).asFile.absoluteFile
            val includeFolder = System.getProperty("voodoo.includeDir")?.asFile ?: rootDir.resolve("include")

            return ScriptCompilationConfiguration(context.compilationConfiguration) {
                annotations.forEach { annotation ->
                    val include = annotation as Include
                    val includedScript = includeFolder.resolve(include.filename)
                    importScripts.append(includedScript.toScriptSource())
                }
            }.asSuccess(reports)
        }
    }
}
