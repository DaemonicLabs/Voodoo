//package voodoo.cli.create
//
//import com.github.ajalt.clikt.core.CliktCommand
//import com.github.ajalt.clikt.core.requireObject
//import com.github.ajalt.clikt.parameters.options.option
//import com.github.ajalt.clikt.parameters.options.required
//import com.github.ajalt.clikt.parameters.options.validate
//import kotlinx.coroutines.runBlocking
//import kotlinx.coroutines.slf4j.MDCContext
//import mu.KotlinLogging
//import mu.withLoggingContext
//import voodoo.cli.CLIContext
//import voodoo.data.flat.ModPack
//import voodoo.pack.MetaPack
//import voodoo.pack.Modpack
//import voodoo.util.VersionComparator
//import voodoo.util.json
//
//class CreateVersionCommand : CliktCommand(
//    name = "version",
//    help = "create a new version"
//) {
//    companion object {
//        private val logger = KotlinLogging.logger {}
//    }
//
//    val cliContext by requireObject<CLIContext>()
//
//    val id by option(
//        "--id",
//        help = "pack id"
//    ).required()
//        .validate {
//            require(it.isNotBlank()) { "id must not be blank" }
//            require(it.matches("""[\w_]+""".toRegex())) { "modpack id must not contain special characters" }
//        }
//
//    val packVersion by option(
//        "--packVersion",
//        help = "pack version"
//    ).required()
//        .validate {  version ->
//            require(version.matches("^\\d+(?:\\.\\d+)+$".toRegex())) {
//                "version must match pattern '^\\d+(\\.\\d+)+\$' eg: 0.1 or 4.11.6 or 1.2.3.4 "
//            }
//        }
//
//    override fun run(): Unit = withLoggingContext("command" to commandName) {
//        runBlocking(MDCContext()) {
//            val rootDir = cliContext.rootDir
//
//            val baseDir = rootDir.resolve(id)
//            val modpackFile = baseDir.resolve(id + "." + Modpack.extension)
//            require(baseDir.exists() && baseDir.isDirectory && modpackFile.exists()) { "$baseDir must exist and be a directory contains ${modpackFile.name}" }
//
////            val versionPacks = baseDir
////                .list { dir, name ->
////                    name.endsWith(".voodoo.json")
////                }!!
////                .map { versionPackFilename ->
////                    val versionPackFile = baseDir.resolve(versionPackFilename)
////
////                    Modpack.parse(versionPackFile)
////                }
////                .sortedWith(compareBy(VersionComparator, Modpack::version))
////
////            require(versionPacks.isNotEmpty()) { "there must be at least one '.voodoo.json' file in the folder $baseDir" }
////            require(versionPacks.none {it.version == packVersion}) { "version $packVersion already exists" }
////
////            // find last version that is smaller than provided new pack version
////            val lastVersionPack = versionPacks.reversed().find { versionPack ->
////                VersionComparator.compare(versionPack.version, packVersion) < 0
////            }
////            require(lastVersionPack != null) { "new version must be larger than at least one existing version" }
//            val modpack = Modpack.parse(modpackFile)
//
//
//            val versionPack = lastVersionPack.copy(
//                version = packVersion
//            )
//
//            val srcFolder = ModPack.srcFolderForVersion(version = versionPack.version, baseFolder = baseDir)
//            require(!srcFolder.exists() || (srcFolder.isDirectory && srcFolder.list()!!.isEmpty())) { "folder $srcFolder must not exist or be a empty directory" }
//            srcFolder.mkdirs()
//
//            // write versionPack
//            versionPack.save(baseDir = baseDir)
//
//            logger.info { "copied ${lastVersionPack.version} to ${versionPack.version}" }
//        }
//    }
//}