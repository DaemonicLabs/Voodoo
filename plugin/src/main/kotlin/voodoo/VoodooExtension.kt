package voodoo

import org.gradle.api.Project
import voodoo.data.CustomTask
import voodoo.data.TaskBuilder
import voodoo.poet.Poet
import voodoo.poet.generator.CurseGenerator
import voodoo.poet.generator.CurseSection
import voodoo.poet.generator.FabricGenerator
import voodoo.poet.generator.ForgeGenerator
import voodoo.util.SharedFolders
import java.io.File

open class VoodooExtension(project: Project) {
    init {
        SharedFolders.RootDir.value = project.rootDir
    }

    var local: Boolean = false
    var localVoodooProjectLocation: File? = null

    internal var tasks: List<CustomTask> = listOf()
        private set

    @Deprecated("use taskBuilder with lambda", ReplaceWith("tasks += CustomTask(name, description) {}"))
    fun addTask(name: String, description: String = "custom task $name", parameters: List<String>) {
        tasks += CustomTask(name, description, parameters)
    }

//    fun add(name: String, description: String = "custom task $name", parameters: List<TaskType>) {
//        tasks += CustomTask(name, description, parameters.map { it.command })
//    }

    fun addTask(name: String, description: String = "custom task $name", taskBuilder: TaskBuilder.() -> Unit = {}) {
        val builder = TaskBuilder()
        builder.taskBuilder()

        tasks += CustomTask(name, description, builder.tasks.map { it.command })
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

    internal val forgeGenerators: MutableList<ForgeGenerator> = mutableListOf()
    fun generateForge(name: String, vararg mcVersions: String) {
        forgeGenerators += ForgeGenerator(name, listOf(*mcVersions))
    }

    internal val fabricGenerators: MutableList<FabricGenerator> = mutableListOf()
    fun generateFabric(name: String, stable: Boolean = true, vararg mcVersions: String) {
        fabricGenerators += FabricGenerator(name, stable, listOf(*mcVersions))
    }

    internal val curseGenerators: MutableList<CurseGenerator> = mutableListOf()
    fun generateCurseforgeMods(
        name: String,
        vararg versions: String,
        categories: List<String> = listOf(),
        slugSanitizer: (String) -> String = Poet::defaultSlugSanitizer
    ) {
        curseGenerators += CurseGenerator(
            name = name,
            section = CurseSection.MODS,
            categories = categories,
            mcVersions =  listOf(*versions),
            slugSanitizer = slugSanitizer
        )
    }
    fun generateCurseforgeTexturepacks(
        name: String,
        vararg versions: String,
        categories: List<String> = listOf(),
        slugSanitizer: (String) -> String = Poet::defaultSlugSanitizer
    ) {
        curseGenerators += CurseGenerator(name, CurseSection.TEXTURE_PACKS, categories, listOf(*versions), slugSanitizer)
    }
}