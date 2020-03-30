package voodoo

import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import voodoo.poet.importer.CurseImporter
import java.io.File

open class CurseImportTask : DefaultTask() {
    @InputDirectory lateinit var rootDir: File
    @InputDirectory lateinit var packsDir: File

    @Input
    @Option(option = "url", description = "modpack download url")
    var url: String? = null

    @Input
    @Option(option = "id", description = "modpack id")
    var id: String? = null

    init {
        group = "voodoo"
    }

    @TaskAction
    fun execImport() {
        if (id == null)
            throw GradleException("id needs to be specified with --id")
        if (url == null)
            throw GradleException("url needs to be specified with --url")

        runBlocking {
            CurseImporter.import(
                id ?: throw GradleException("id was null"),
                url ?: throw GradleException("url was null"),
                rootDir,
                packsDir
            )
        }
    }
}
