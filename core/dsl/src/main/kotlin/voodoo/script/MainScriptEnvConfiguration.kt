package voodoo.script

import java.io.File
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.javaHome
import kotlin.script.experimental.jvm.jvm

object MainScriptEnvConfiguration : ScriptCompilationConfiguration({
    defaultImports(
        "voodoo.*",
        "voodoo.dsl.*",
        "voodoo.provider.*"
    )
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
        javaHome(File("/usr/lib/jvm/intellij-jdk"))
    }
})