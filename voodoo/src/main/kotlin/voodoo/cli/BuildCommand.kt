package voodoo.cli

import com.eyeem.watchadoin.Stopwatch
import com.eyeem.watchadoin.saveAsHtml
import com.eyeem.watchadoin.saveAsSvg
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.builder.Builder
import voodoo.config.Configuration
import voodoo.data.lock.LockPack
import voodoo.pack.MetaPack
import voodoo.pack.VersionPack
import voodoo.util.VersionComparator
import voodoo.util.json

class BuildCommand() : CliktCommand(
    name = "build",
    help = ""
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    val cliContext by requireObject<CLIContext>()

    val id by option(
        "--id",
        help = "pack id"
    ).required()
        .validate {
            require(it.isNotBlank()) { "id must not be blank" }
            require(it.matches("""[\w_]+""".toRegex())) { "modpack id must not contain special characters" }
        }

    val versionOptions by option(
        "--version",
        help = "build only specified versions"
    ).multiple().validate { version ->
        require(version.all { it.matches("^\\d+(?:\\.\\d+)+$".toRegex()) }) {
            "all versions must match pattern '^\\d+(\\.\\d+)+\$' eg: 0.1 or 4.11.6 or 1.2.3.4 "
        }
    }

    val all by option("--all").flag("--newest", "--latest", default = true, defaultForHelp = "only $commandName newest version")
    val buildMissing by option(
        "--buildMissing",
        help = "build versions when lockfiles are missing"
    ).flag("--skipMissing", default = true)
//    val rebuildFailing by option(
//        "--rebuildFailing",
//        help = "rebuild versions that failed to parse lockfiles, this might overwrite old files"
//    ).flag(default = false)

    override fun run() = withLoggingContext("command" to commandName, "pack" to id) {
        runBlocking(MDCContext()) {
            val stopwatch = Stopwatch(commandName)

            val rootDir = cliContext.rootDir

            require(id.isNotBlank()) { "id must not be blank" }

            stopwatch {

                val config = Configuration.parse(rootDir = rootDir)

                val baseDir = rootDir.resolve(id)

                val metaPackFile = baseDir.resolve(MetaPack.FILENAME)
                val metaPack = json.decodeFromString(MetaPack.serializer(), metaPackFile.readText())

                val versionPacks = VersionPack.parseAll(baseDir = baseDir)
                    .sortedWith(compareBy(VersionComparator, VersionPack::version))
                    .let { versionPacks ->
                        val latestVersion = versionPacks.last().version
                        versionPacks.filter { versionPack ->
                            when {
                                // versionOptions is set
                                versionOptions.isNotEmpty() -> {
                                    // version matches
                                    versionOptions.any { versionOption ->
                                        VersionComparator.compare(versionOption, versionPack.version) == 0
                                    }
                                }
                                // --all versions are getting rebuilt
                                all -> return@filter true

                                // --version is not set and latest version is getting built
                                versionOptions.isEmpty() && versionPack.version == latestVersion -> return@filter true
                                else -> {
                                    val lockPackFile = LockPack.fileForVersion(version = versionPack.version, baseDir = baseDir)
                                    when {
                                        // build missing lockfiles
                                        buildMissing && !lockPackFile.exists() -> true
//                                        // rebuild parse failures for versions in
//                                        rebuildFailing && lockPackFile.exists() -> {
//                                            try {
//                                                LockPack.parse(lockPackFile, rootDir)
//                                                // lockpack parsed successfully, can be skipped
//                                                false
//                                            } catch (e: Exception) {
//                                                e.printStackTrace()
//                                                // lockpack failed parsing, it will be rebuilt
//                                                true
//                                            }
//                                        }
                                        else -> false
                                    }
                                }
                            }

                        }
                    }

                versionPacks.forEach { pack ->
                    val otherpacks = versionPacks - pack
                    require(
                        otherpacks.all { other ->
                            pack.version != other.version
                        }
                    ) { "version ${pack.version} is not unique" }
                    require(
                        pack.packageConfiguration.voodoo.relativeSelfupdateUrl == null || otherpacks.all { other ->
                            pack.packageConfiguration.voodoo.relativeSelfupdateUrl != other.packageConfiguration.voodoo.relativeSelfupdateUrl
                        }
                    ) { "relativeSelfupdateUrl ${pack.packageConfiguration.voodoo.relativeSelfupdateUrl} is not unique" }
                }

                val lockPacks = versionPacks
                    .sortedWith(compareBy(VersionComparator, VersionPack::version))
                    .map { versionPack ->
                        withLoggingContext("version" to versionPack.version) {
                            withContext(MDCContext()) {
                                val modpack = versionPack.flatten(
                                    rootDir = rootDir,
                                    id = id,
                                    overrides = config.overrides,
                                    metaPack = metaPack
                                )
//                                logger.debug { "modpack: $modpack" }
                                logger.debug { "entrySet: ${modpack.entrySet}" }


                                val lockPack = Builder.lock("build v${modpack.version}".watch, modpack)

                                lockPack
                            }
                        }
                    }



//                val tomeEnv = TomeEnv(
//                    rootDir.resolve("docs")
//                ).apply {
//                    add("modlist.md", ModlistGeneratorMarkdown)
//                }

//                    if (tomeEnv != null) {
//                        val uploadDir = SharedFolders.UploadDir.get(id)
//                        val rootDir = SharedFolders.RootDir.get().absoluteFile
//
//                        // TODO: merge tome into core
//                        "tome".watch {
//                            Tome.generate(this, lockPack, tomeEnv, uploadDir)
//                        }
//
//                        // TODO: just generate meta info
//
//                        Diff.writeMetaInfo(
//                            stopwatch = "writeMetaInfo".watch,
//                            rootDir = rootDir.absoluteFile,
//                            newPack = lockPack
//                        )
//                    }
                //TODO: fold lockpacks
            }

            val reportDir = rootDir.resolve("reports").apply { mkdirs() }
            stopwatch.saveAsSvg(reportDir.resolve("${id}_build.report.svg"))
            stopwatch.saveAsHtml(reportDir.resolve("${id}_build.report.html"))

        }
    }
}