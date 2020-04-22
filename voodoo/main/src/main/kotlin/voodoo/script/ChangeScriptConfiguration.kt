package voodoo.script

import voodoo.script.annotation.Include
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.refineConfiguration

object ChangeScriptConfiguration : ScriptCompilationConfiguration({
    defaultImports.append(
        "voodoo.data.meta.MetaInfo",
        "voodoo.changelog.ChangelogBuilder"
    )

//    compilerOptions.append("-jvm-target 1.8")

    refineConfiguration {
        onAnnotations<Include>(Include.Companion::configureIncludes)
    }
})
