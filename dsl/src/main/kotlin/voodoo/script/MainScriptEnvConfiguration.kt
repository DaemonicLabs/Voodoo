package voodoo.script

import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports

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

    // TODO: evaluate file level annotations
//    refineConfiguration {
//        onAnnotations(DependsOn::class, Repository::class, handler = ::configureMavenDepsOnAnnotations)
//    }
})

