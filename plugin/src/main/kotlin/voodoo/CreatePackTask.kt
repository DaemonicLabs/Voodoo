package voodoo

import id
import kotlinx.coroutines.experimental.runBlocking
import list
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import projectID
import rootEntry
import voodoo.curse.CurseClient
import voodoo.data.curse.CurseConstants
import voodoo.forge.ForgeUtil
import voodoo.provider.CurseProvider
import java.io.File

open class CreatePackTask() : DefaultTask() {
    lateinit var packs: File

    @Input
    @Option(option = "id", description = "modpack id")
    var id: String? = null

    @Input
    @Option(option = "title", description = "modpack title (optional)")
    var titleStr: String? = null

    @Input
    @Option(option = "mcVersion", description = "minecraft version")
    var mcVersion: String? = null

    init {
        group = "voodoo"
    }

    @TaskAction
    fun create() {
        if (id == null)
            throw GradleException("id needs to be specified with --id")
        if (mcVersion == null)
            throw GradleException("mcVersion needs to be specified with --mcVersion")

        val modIdentifiers = runBlocking {
            Poet.requestMods()
        }.mapKeys { (key, _) ->
            Poet.defaultSlugSanitizer(key)
        }.toList().shuffled().take(10)

        val filteredMods = runBlocking {
            modIdentifiers.filter { (_, projectId) ->
                val files = CurseClient.getAllFilesForAddon(projectId, CurseConstants.PROXY_URL)
                files.any { file -> file.gameVersion.contains(mcVersion) }
            }
        }
        val forgeData = runBlocking {
            ForgeUtil.deferredData.await()
        }

        val nestedPack = MainEnv(rootDir = packs).nestedPack(
            id = id ?: throw GradleException("id was null"),
            mcVersion = mcVersion ?: throw GradleException("mcVersion was null")
        ) {
            title = titleStr ?: id!!.capitalize()
            authors = listOf(System.getProperty("user.name"))
            forge = forgeData.promos["$mcVersion-recommended"]
            root = rootEntry(CurseProvider) {
                list {
                    filteredMods.forEach { (identifier, projectId) ->
                        id(identifier) {
                            projectID = projectId
                        }
                    }
                }
            }
        }

        NewModpack.createModpack(
            folder = packs,
            nestedPack = nestedPack
        )
    }
}
