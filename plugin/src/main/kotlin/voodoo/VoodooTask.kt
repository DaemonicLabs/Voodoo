package voodoo

import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

open class VoodooTask() : JavaExec() {
//    @Input
//    val tasks: MutableList<List<String>> = mutableListOf()

    @Input
    @Option(option = "script", description = "voodoo script file")
    var scriptFile: File? = null

    init {
        group = "voodoo"
    }

    @TaskAction
    override fun exec() {
        if (scriptFile == null) {
            throw GradleException("--script was not set")
        }
        val fullArgs = mutableListOf(scriptFile!!.path)
        args?.let {
            fullArgs.addAll(it)
        }
        args = fullArgs
        println("executing: $args")

        super.exec()
    }
}