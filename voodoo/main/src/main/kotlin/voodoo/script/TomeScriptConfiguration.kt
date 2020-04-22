package voodoo.script

import voodoo.script.annotation.Include
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.refineConfiguration

object TomeScriptConfiguration : ScriptCompilationConfiguration({
    defaultImports.append(
        "voodoo.*",
        "voodoo.dsl.*",
        "voodoo.data.flat.*",
        "voodoo.data.lock.*",
        "voodoo.tome.*",
        "voodoo.provider.*"
    )

    compilerOptions.append("-jvm-target 1.8")

    refineConfiguration {
        onAnnotations<Include>(Include.Companion::configureIncludes)
    }
})
