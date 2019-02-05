package voodoo.script

import voodoo.Include
import voodoo.Include.Companion.configureIncludes
import voodoo.poet.Poet
import voodoo.util.asFile
import kotlin.script.experimental.api.ScriptCollectedData
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.foundAnnotations
import kotlin.script.experimental.api.importScripts
import kotlin.script.experimental.api.refineConfiguration
import kotlin.script.experimental.host.toScriptSource

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

