package voodoo

import org.gradle.api.Project
import java.io.File

open class VoodooExtension(project: Project) {
    var generatedSource: File = project.file(".voodoo")
    var packDirectory: File = project.file("packs")
}