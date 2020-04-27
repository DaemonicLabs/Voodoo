package voodoo

import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

// TODO: encode arguments to voodoo as json
open class VoodooTask : JavaExec() {
    @Input
    @Option(option = "script", description = "voodoo script file")
    var scriptFile: String? = null

    init {
        group = "voodoo"
        main = "voodoo.VoodooMain"
    }

    @TaskAction
    override fun exec() {
        if (scriptFile == null) {
            throw GradleException("--script was not set")
        }
        if (args?.isEmpty() != false) {
            throw GradleException("--args is not set")
        }
        val fullArgs = mutableListOf(scriptFile!!)
        logger.lifecycle("adding arguments to $fullArgs")
        logger.lifecycle("adding $args")
        args?.takeIf { it.isNotEmpty() }?.let {
            fullArgs.addAll(it)
            args = fullArgs
        }
        logger.lifecycle("executing: $args")
        logger.lifecycle("workingDir: $workingDir")

        super.exec()
    }
}
