package moe.nikky.voodoo

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
open class PoetTask : DefaultTask() {
    @OutputDirectory
    var targetFolder: File = project.file(".voodoo")

    init {
        group = "build"
        description = "Generates Curse and Forge Constants"
    }

    @TaskAction
    fun runPoet() {
        targetFolder.mkdirs()
        poet(root = targetFolder)

    }
}