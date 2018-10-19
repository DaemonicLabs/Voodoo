package voodoo

import org.gradle.api.Project
import java.io.File

open class VoodooExtension(project: Project) {
    var rootDir: File = project.rootDir
    var generatedSource: File = rootDir.resolve(".voodoo")
    var packDirectory: File = rootDir.resolve("packs")
}