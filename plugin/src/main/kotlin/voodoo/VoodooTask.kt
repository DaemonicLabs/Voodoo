package voodoo

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class VoodooTask : JavaExec() {
    init {
        group = "voodoo"
    }

    @TaskAction
    override fun exec() {
        args = (args ?: mutableListOf()) + ""

        super.exec()
    }
}