package voodoo.script

import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.defaultImports

object MainScriptEnvConfiguration : ScriptCompilationConfiguration({
    defaultImports.append(
        "voodoo.*",
        "voodoo.dsl.*",
        "voodoo.provider.*"
    )
})