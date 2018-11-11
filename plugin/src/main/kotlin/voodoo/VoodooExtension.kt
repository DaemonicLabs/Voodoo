package voodoo

import org.gradle.api.Project
import voodoo.data.CustomTask
import java.io.File

open class VoodooExtension(project: Project) {
    var rootDir: File = project.rootDir

    fun generatedSource(resolver: (rootDir: File) -> File) {
        generatedSourceCall = resolver
    }

    fun packDirectory(resolver: (rootDir: File) -> File) {
        packDirectoryCall = resolver
    }

    internal var tasks: List<CustomTask> = listOf()
        private set

    fun addTask(name: String, description: String = "custom task $name", parameters: List<String>) {
        tasks += CustomTask(name, description, parameters)
    }

    private var generatedSourceCall: (rootDir: File) -> File = { rootDir.resolve(".voodoo") }
    private var packDirectoryCall: (rootDir: File) -> File = { rootDir.resolve("packs") }

    internal val getGeneratedSrc: File get() = generatedSourceCall(rootDir)
    internal val getPackDir: File get() = packDirectoryCall(rootDir)
}