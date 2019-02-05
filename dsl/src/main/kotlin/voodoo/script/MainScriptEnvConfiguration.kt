package voodoo.script

import voodoo.Include
import voodoo.poet.Poet
import voodoo.util.asFile
import kotlin.script.experimental.api.ScriptCollectedData
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
        // TODO: add and evaluate file level annotations
        val reports: MutableList<ScriptDiagnostic> = mutableListOf()
        beforeParsing { context ->
            val rootDir = (System.getProperty("voodoo.rootDir") ?: System.getProperty("user.dir")).asFile.absoluteFile
            val generatedFilesDir =
                System.getProperty("voodoo.generatedSrc")?.asFile ?: rootDir.resolve("build").resolve(".voodoo").absoluteFile
            val generatedFiles = Poet.generateAll(rootDir = rootDir, generatedSrcDir = generatedFilesDir)

            ScriptCompilationConfiguration(context.compilationConfiguration) {
                importScripts.append(generatedFiles.map { it.toScriptSource() })
//                reports += ScriptDiagnostic("test", ScriptDiagnostic.Severity.ERROR)
            }.asSuccess(reports)
        }

        onAnnotations<Include>(Include.Companion::configureIncludes)
    }
})

