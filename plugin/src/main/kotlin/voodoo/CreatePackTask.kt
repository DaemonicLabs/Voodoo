package voodoo

import kotlinx.coroutines.runBlocking
import list
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import voodoo.curse.CurseClient
import voodoo.data.curse.CurseConstants
import voodoo.data.curse.Section
import voodoo.dsl.ID
import voodoo.forge.ForgeUtil
import voodoo.poet.Poet
import voodoo.poet.PoetPack
import voodoo.provider.CurseProvider
import voodoo.script.MainScriptEnv
import java.io.File

open class CreatePackTask : DefaultTask() {
    lateinit var rootDir: File
    lateinit var packsDir: File

    @Input
    @Option(option = "id", description = "modpack id")
    var id: String = "rename_me"

    @Input
    @Option(option = "title", description = "modpack title (optional)")
    var titleStr: String = ""

    @Input
    @Option(option = "mcVersion", description = "minecraft version")
    var mcVersion: String = "1.12.2"

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
            Poet.request(section = Section.MODS, gameVersions = listOf(mcVersion))
        }.mapKeys { (key, _) ->
            Poet.defaultSlugSanitizer(key)
        }.toList().shuffled().take(10)

        val randomMods = runBlocking {
            modIdentifiers.filter { (_, projectId) ->
                val files = CurseClient.getAllFilesForAddon(projectId, CurseConstants.PROXY_URL)
                files.any { file -> file.gameVersion.contains(mcVersion) }
            }
        }
        val forgeData = runBlocking {
            ForgeUtil.deferredData.await()
        }

        val scriptEnv = MainScriptEnv(
            rootDir = rootDir,
            id = id ?: throw GradleException("id was null")
        ).apply {
            mcVersion = this@CreatePackTask.mcVersion ?: throw GradleException("mcVersion was null")
            title = titleStr.takeIf { it.isNotBlank() } ?: id.capitalize()
            authors = listOf(System.getProperty("user.name"))
            forge = forgeData.promos["$mcVersion-recommended"]
            root(CurseProvider) {
                list {
                    randomMods.forEach { (identifier, projectId) ->
                        +ID(projectId.value) configure {
                            //                            projectID = projectId
                        }
                    }
                }
            }
        }

        val nestedPack = scriptEnv.pack

        PoetPack.createModpack(
            folder = packsDir,
            nestedPack = nestedPack
        )
    }
}
