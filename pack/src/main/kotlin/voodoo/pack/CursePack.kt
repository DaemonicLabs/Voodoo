package voodoo.pack

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.html.ATarget
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.html
import kotlinx.html.li
import kotlinx.html.stream.createHTML
import kotlinx.html.ul
import kotlinx.serialization.json.JSON
import voodoo.data.Side
import voodoo.data.curse.CurseFile
import voodoo.data.curse.CurseManifest
import voodoo.data.curse.CurseMinecraft
import voodoo.data.curse.CurseModLoader
import voodoo.data.lock.LockPack
import voodoo.forge.ForgeUtil
import voodoo.provider.CurseProvider
import voodoo.provider.Providers
import voodoo.util.packToZip

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

object CursePack : AbstractPack() {
    override val label = "SK Packer"

    override suspend fun pack(
        modpack: LockPack,
        target: String?,
        clean: Boolean
    ) {
        val cacheDir = directories.cacheHome
        val workspaceDir = modpack.rootDir.resolve(".curse")
        val modpackDir = workspaceDir.resolve(with(modpack) { "$id-$version" })
        val srcFolder = modpackDir.resolve("overrides")

        if (clean) {
            logger.info("cleaning modpack directory $srcFolder")
            srcFolder.deleteRecursively()
        }
        if (!srcFolder.exists()) {
            logger.info("copying files into overrides")
            val mcDir = modpack.sourceFolder
            if (mcDir.exists()) {
                mcDir.copyRecursively(srcFolder, overwrite = true)
            } else {
                logger.warn("minecraft directory $mcDir does not exist")
            }
        }

        for (file in srcFolder.walkTopDown()) {
            when {
                file.name == "_SERVER" -> file.deleteRecursively()
                file.name == "_CLIENT" -> file.renameTo(file.parentFile)
            }
        }

        val loadersFolder = modpackDir.resolve("loaders")
        logger.info("cleaning loaders $loadersFolder")
        loadersFolder.deleteRecursively()

        coroutineScope {
            val jobs = mutableListOf<Job>()

            val forgeVersion = ForgeUtil.forgeVersionOf(modpack.forge)?.forgeVersion

            val modsFolder = srcFolder.resolve("mods")
            logger.info("cleaning mods $modsFolder")
            modsFolder.deleteRecursively()

            val curseModsChannel = Channel<CurseFile>(Channel.CONFLATED)
            val pool = newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors() + 1, "pool")

            // download entries
            for (entry in modpack.entrySet) {
                if (entry.side == Side.SERVER) continue
                jobs += launch(context = coroutineContext + pool) {
                    val folder = entry.serialFile.absoluteFile.parentFile
                    val required = modpack.features.none { feature ->
                        feature.entries.any { it == entry.id }
                    }

                    val provider = Providers[entry.provider]
                    if (provider == CurseProvider) {
                        curseModsChannel.send(
                            CurseFile(
                                entry.projectID,
                                entry.fileID,
                                required
                            ).also {
                                println("added curse file $it")
                            }
                        )
                    } else {
                        val targetFolder = srcFolder.resolve(folder)
                        val (_, file) = provider.download(entry, targetFolder, cacheDir)
                        if (!required) {
                            val optionalFile = file.parentFile.resolve(file.name + ".disabled")
                            file.renameTo(optionalFile)
                        }
                    }
                }
                logger.info("started job: download '${entry.id}'")
                delay(100)
            }

            delay(100)
            logger.info("waiting for jobs to finish")
            curseModsChannel.consume {
                jobs.joinAll()
            }

            val curseMods = curseModsChannel.toList()

            // generate modlist
            val modListFile = modpackDir.resolve("modlist.html")

            val html = createHTML().html {
                body {
                    ul {
                        for (entry in modpack.entrySet.sortedBy { it.name.toLowerCase() }) {
                            val provider = Providers[entry.provider]
                            if (entry.side == Side.SERVER) {
                                continue
                            }
                            val projectPage =
                                runBlocking { provider.getProjectPage(entry) }
                            val authors = runBlocking { provider.getAuthors(entry) }
                            val authorString = if (authors.isNotEmpty()) " (by ${authors.joinToString(", ")})" else ""

                            li {
                                when {
                                    projectPage.isNotEmpty() -> a(href = projectPage) { +"${entry.name} $authorString" }
                                    entry.url.isNotBlank() -> {
                                        +"direct: "
                                        a(href = entry.url, target = ATarget.blank) { +"${entry.name} $authorString" }
                                    }
                                    else -> {
                                        val source =
                                            if (entry.fileSrc.isNotBlank()) "file://" + entry.fileSrc else "unknown"
                                        +"${entry.name} $authorString (source: $source)"
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (modListFile.exists()) modListFile.delete()
            modListFile.createNewFile()
            modListFile.writeText(html)

            val curseManifest = CurseManifest(
                name = modpack.title,
                version = modpack.version,
                author = modpack.authors.joinToString(", "),
                minecraft = CurseMinecraft(
                    version = modpack.mcVersion,
                    modLoaders = forgeVersion?.run {
                        listOf(
                            CurseModLoader(
                                id = "forge-$forgeVersion",
                                primary = true
                            )
                        )
                    } ?: emptyList()
                ),
                manifestType = "minecraftModpack",
                manifestVersion = 1,
                files = curseMods,
                overrides = "overrides"
            )
            val json = JSON(indented = true)
            val manifestFile = modpackDir.resolve("manifest.json")
            manifestFile.writeText(json.stringify(CurseManifest.serializer(), curseManifest))

            val cursePackFile = workspaceDir.resolve(with(modpack) { "$id-$version.zip" })

            packToZip(modpackDir.toPath(), cursePackFile.toPath())

            logger.info("packed ${modpack.id} -> $cursePackFile")
        }
    }
}