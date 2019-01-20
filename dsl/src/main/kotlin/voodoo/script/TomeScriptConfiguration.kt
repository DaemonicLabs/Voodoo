package voodoo.script

import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports

object TomeScriptConfiguration : ScriptCompilationConfiguration({
    defaultImports.append(
        "voodoo.*",
        "voodoo.dsl.*",
        "voodoo.data.flat.*",
        "voodoo.data.lock.*",
        "voodoo.provider.*"
    )

    compilerOptions.append("-jvm-target 1.8")
})

