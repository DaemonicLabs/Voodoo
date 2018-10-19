package voodoo

import org.gradle.api.Project
import java.io.File

open class VoodooExtension(project: Project) {
    var rootDir: File = project.rootDir
    var generatedSource: (rootDir: File) -> File = { rootDir.resolve(".voodoo") }
    var packDirectory: (rootDir: File) -> File =  { rootDir.resolve("packs") }

    internal val actualGeneratedSrc: File get() = generatedSource(rootDir)
    internal val actualPackDir: File get() = packDirectory(rootDir)
}