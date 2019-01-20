package voodoo.script

import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports

object MainScriptEnvConfiguration : ScriptCompilationConfiguration({
    defaultImports.append(
        "voodoo.*",
        "voodoo.dsl.*",
        "voodoo.provider.*",
        "voodoo.data.Side",
        "voodoo.data.UserFiles",
        "voodoo.data.curse.FileType",
        "voodoo.provider.CurseProvider",
        "voodoo.provider.JenkinsProvider"
    )
        // does not work dynamical or relative
//    jvm {
//        importScripts.append(
//            File("./.voodoo/Constants.kt").toScriptSource(),
//            File("./.voodoo/Forge.kt").toScriptSource(),
//            File("./.voodoo/Mod.kt").toScriptSource(),
//            File("./.voodoo/TexturePack.kt").toScriptSource()
//        )
//    }
    compilerOptions.append("-jvm-target 1.8")
//    refineConfiguration {
//        onAnnotations(DependsOn::class, Repository::class, handler = ::configureMavenDepsOnAnnotations)
//    }
})

