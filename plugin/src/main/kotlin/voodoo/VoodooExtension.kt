package voodoo

import org.gradle.api.Project
import voodoo.data.CustomTask
import java.io.File

open class VoodooExtension(project: Project) {
    fun rootDir(resolver: () -> File) {
        rootDirResolver = resolver
    }

    fun packDirectory(resolver: (rootDir: File) -> File) {
        packDirectoryResolver = resolver
    }

    fun tomeDirectory(resolver: (rootDir: File) -> File) {
        tomeDirectoryResolver = resolver
    }

    fun generatedSource(resolver: (rootDir: File, id: String) -> File) {
        generatedSourceResolver = resolver
    }

    fun uploadDirectory(resolver: (rootDir: File, id: String) -> File) {
        uploadDirectoryResolver = resolver
    }

    fun docDirectory(resolver: (rootDir: File, id: String) -> File) {
        docDirectoryResolver = resolver
    }

    var local: Boolean = false
    val localVoodooProjectLocation: File = project.rootDir.parentFile

    internal var tasks: List<CustomTask> = listOf()
        private set

    fun addTask(name: String, description: String = "custom task $name", parameters: List<String>) {
        tasks += CustomTask(name, description, parameters)
    }

    private var rootDirResolver: () -> File = { project.rootDir }
    private var packDirectoryResolver: (rootDir: File) -> File = { rootDir -> rootDir.resolve("packs") }
    private var tomeDirectoryResolver: (rootDir: File) -> File = { rootDir -> rootDir.resolve("tome") }
    private var generatedSourceResolver: (rootDir: File, id: String) -> File = { rootDir, id -> rootDir.resolve("build").resolve(".voodoo") }
    private var uploadDirectoryResolver: (rootDir: File, id: String) -> File = { rootDir, id -> rootDir.resolve("_upload") }
    private var docDirectoryResolver: (rootDir: File, id: String) -> File = { rootDir, id -> getUploadDir(id).resolve(id).resolve("docs") }

    internal fun getRootDir(): File = rootDirResolver()
    internal fun getPackDir(): File = packDirectoryResolver(getRootDir())
    internal fun getTomeDir(): File = tomeDirectoryResolver(getRootDir())
    internal fun getGeneratedSrc(id: String): File = generatedSourceResolver(getRootDir(), id)
    internal fun getUploadDir(id: String): File = uploadDirectoryResolver(getRootDir(), id)
    internal fun getDocDir(id: String): File  = docDirectoryResolver(getRootDir(), id)
}