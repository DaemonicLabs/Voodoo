package voodoo

import org.gradle.api.Project
import java.io.File

open class VoodooExtension(project: Project) {
    var rootDir: File = project.rootDir

    fun generatedSource(resolver: (rootDir: File) -> File) {
        generatedSourceCall = resolver
    }

    fun packDirectory(resolver: (rootDir: File) -> File) {
        packDirectoryCall = resolver
    }

    private var generatedSourceCall: (rootDir: File) -> File = { rootDir.resolve(".voodoo") }
    private var packDirectoryCall: (rootDir: File) -> File = { rootDir.resolve("packs") }

    internal val getGeneratedSrc: File get() = generatedSourceCall(rootDir)
    internal val getPackDir: File get() = packDirectoryCall(rootDir)
}