package voodoo

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import voodoo.poet.Poet
import voodoo.util.SharedFolders
import java.io.File

@CacheableTask
open class PoetTask : DefaultTask() {
    @OutputDirectory
    var targetFolder: File = SharedFolders.GeneratedSrc.get()

    init {
        group = "build"
        description = "Generates Curse and Forge Constants"
    }

    @TaskAction
    fun runPoet() {
        targetFolder.mkdirs()
        Poet.generateAll(generatedSrcDir = targetFolder)
    }
}