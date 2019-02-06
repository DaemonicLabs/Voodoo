package voodoo.script

import voodoo.Include
import voodoo.poet.Poet
import voodoo.util.SharedFolders
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.importScripts
import kotlin.script.experimental.api.refineConfiguration
import kotlin.script.experimental.host.toScriptSource

object MainScriptEnvConfiguration : ScriptCompilationConfiguration({
    defaultImports.append(
        "voodoo.*",
        "voodoo.dsl.*",
        "voodoo.provider.*",
        "voodoo.data.*",
        "voodoo.data.curse.*",
        "voodoo.provider.*",
        "com.skcraft.launcher.model.modpack.Recommendation"
    )
    compilerOptions.append("-jvm-target 1.8")

    refineConfiguration {
        val reports: MutableList<ScriptDiagnostic> = mutableListOf()

        onAnnotations<Include>(Include.Companion::configureIncludes)

        beforeCompiling { context ->
//            println("context.collectedData: '${context.collectedData}' ")
//            context.collectedData?.entries()?.forEach { (key, value) ->
//                println("collectedData $key: '$value' ")
//            }
//            context.compilationConfiguration.entries().forEach { (key, value) ->
//                println("compilationConfiguration $key: '$value' ")
//            }

            val generatedFilesDir = SharedFolders.GeneratedSrc.get().absoluteFile
            val generatedFiles = Poet.generateAll(generatedSrcDir = generatedFilesDir)

            val compilationConfiguration = ScriptCompilationConfiguration(context.compilationConfiguration) {
                importScripts.append(generatedFiles.map { it.toScriptSource() })
                reports += ScriptDiagnostic("adding to importedScripts: $generatedFiles", ScriptDiagnostic.Severity.INFO)
            }

//            compilationConfiguration.entries().forEach {
//                println("beforeCompiling    $it")
//            }

            compilationConfiguration.asSuccess(reports)
        }
    }
})
