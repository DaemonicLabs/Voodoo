package voodoo.script

import java.io.File
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.displayName
import kotlin.script.experimental.api.fileExtension
import kotlin.script.experimental.api.refineConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.javaHome
import kotlin.script.experimental.jvm.jvm

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