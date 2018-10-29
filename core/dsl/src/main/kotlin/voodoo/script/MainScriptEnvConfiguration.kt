package voodoo.script

import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.defaultImports

object MainScriptEnvConfiguration : ScriptCompilationConfiguration({
    defaultImports.append(
        "voodoo.*",
        "voodoo.dsl.*",
        "voodoo.provider.*"
    )
//    compilerOptions.append("jvm-target", "1.8")
//    jvm {
//        dependenciesFromCurrentContext(wholeClasspath = true)
//        javaHome(File("/usr/lib/jvm/intellij-jdk")) // TODO use environment variable
//    }
})