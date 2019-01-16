package voodoo.script

import java.io.File
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.dependenciesSources
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.api.refineConfiguration
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.jvm

object MainScriptEnvConfiguration : ScriptCompilationConfiguration({
    defaultImports.append(
        "voodoo.*",
        "voodoo.dsl.*",
        "voodoo.provider.*"
    )
//    jvm {
//
//    }
//    ide {
//        dependenciesSources.append(
//            JvmDependency(
//                File(".voodoo/Constants.kt"),
//                File(".voodoo/Forge.kt"),
//                File(".voodoo/Mod.kt"),
//                File(".voodoo/TexturePack.kt")
//            )
//        )
//    }
    compilerOptions.append("-jvm-target 1.8")
//    refineConfiguration {
//        onAnnotations(DependsOn::class, Repository::class, handler = ::configureMavenDepsOnAnnotations)
//    }
})

