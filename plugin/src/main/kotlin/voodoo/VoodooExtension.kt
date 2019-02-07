package voodoo

import org.gradle.api.Project
import voodoo.data.CustomTask
import voodoo.util.SharedFolders
import java.io.File

open class VoodooExtension(project: Project) {
    init {
        SharedFolders.RootDir.default = project.rootDir
    }

    var local: Boolean = false
    val localVoodooProjectLocation: File = project.rootDir.parentFile

    internal var tasks: List<CustomTask> = listOf()
        private set

    fun addTask(name: String, description: String = "custom task $name", parameters: List<String>) {
        tasks += CustomTask(name, description, parameters)
    }

    fun rootDir(resolver: () -> File) {
        SharedFolders.RootDir.resolver = resolver
    }

    fun packDirectory(resolver: (rootDir: File) -> File) {
        SharedFolders.PackDir.resolver = resolver
    }

    fun tomeDirectory(resolver: (rootDir: File) -> File) {
        SharedFolders.TomeDir.resolver = resolver
    }

    fun includeDirectory(resolver: (rootDir: File) -> File) {
        SharedFolders.IncludeDir.resolver = resolver
    }

    fun generatedSource(resolver: (rootDir: File, id: String) -> File) {
        SharedFolders.GeneratedSrc.resolver = resolver
    }

    fun uploadDirectory(resolver: (rootDir: File, id: String) -> File) {
        SharedFolders.UploadDir.resolver = resolver
    }

    fun docDirectory(resolver: (rootDir: File, id: String) -> File) {
        SharedFolders.DocDir.resolver = resolver
    }
}