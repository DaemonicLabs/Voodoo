package moe.nikky.voodoo.format

import Modloader
import kotlinx.serialization.json.Json
import moe.nikky.voodoo.format.builder.PropertiesApplicator
import moe.nikky.voodoo.format.modpack.Manifest
import moe.nikky.voodoo.format.modpack.entry.FileInstall
import moe.nikky.voodoo.format.modpack.entry.Side
import mu.KLogging
import voodoo.util.toRelativeUnixPath
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object PackageBuilder : KLogging() {

    /***
     * @param inputPath folder with modpack source
     * @param outputPath target folder for output, will not delete existing files
     * @param modpackId ascii identifier for the modpack, used in filenames
     * @param modpackTitle Human readable Title
     * @param modpackUniqueVersion semver or similar version string, has to differ from all previously used versions
     * @param gameVersion minecraft version
     * @param modLoader modloader to use, fabric/forge
     * @param userFiles pattern to include / exclude userFiles
     * @param features list of features
     * @param prettyPrint whether to serialize entries with indentation in json
     */
    fun build(
        inputPath: File,
        outputPath: File,
        modpackId: String,
        installerLocation: String,
        modpackTitle: String = modpackId,
        modpackVersion: String,
        gameVersion: String,
        modLoader: Modloader,
        objectsLocation: String = "objects",
        userFiles: FnPatternList = FnPatternList(),
        features: List<FeatureWithPattern> = listOf(),
        prettyPrint: Boolean = true
    ) {
        val manifestDest: File = outputPath.resolve("${modpackId}_${modpackVersion}.json")
        val versionlistingFile: File = outputPath.resolve("$modpackId.json")

        val uniqueVersion = "$modpackVersion." + DateTimeFormatter
            .ofPattern("yyyyMMddHHmm")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())

        // TODO verify gameVersion
        // TODO regex check modpackId
        // TODO validate all user input


        val filesDir = inputPath.resolve(DEFAULT_SRC_DIRNAME)
        val objectsDir = outputPath.resolve(objectsLocation)
        val json: Json = Json {
            this.prettyPrint = prettyPrint
            encodeDefaults = false
            serializersModule
        }

        // create PackageBuilder : skip

        // readConfig (modpack.json)
        // reads options from file if exists
        // name, title, gameVersion, launchModifier, feature, userFiles
        // TODO: pass userFile info into function

        // readVersionManifest  /version.json : skip
        // seems to be for providing a custom versionManifest, skip that


        // TODO: temporarily pass features as argument to function
        // scan /src/
        // scans files and then registers all patterns with FileInfoScanner
        // scans for .info.json files and registers them as features (in manifest)
        // TODO: redo .. smarter ?
        // used to be `scanner = FileInfoScanner()` and `scanner.patterns`
        val applicator = PropertiesApplicator(userFiles = userFiles)
        for (feature in features) {
            applicator.register(feature)
        }


        // addFiles (/src,
        // collects clientFiles
        // detects .url.txt files and downloads them
        // generates sha-1 checksums
        // generates `to` path (based on checksum) and copies it there
        // creates FileInstall entries and adds them to manifest
        // TODO: also detect sides based on parentfolders containing _CLIENT or _SERVER, ony one allowed

        val tasks = addFiles(
            filesDir = filesDir,
            destDir = objectsDir,
            onEntry = applicator::apply
        )

        // addLoaders : skip / redo
        // TODO: pass in or read forge/fabric modloader from config

        // downloadLibraries: skip, we do not need that

        // writeManifest ($manifestDest)
        // validate maifest
        //    !name.isNullOrEmpty
        //    !gameVersion.isNullOoEmpty
        // takes features from applicator.featuresInUse (see scan step?)
        // serialize manifest to $manfiestDest
        logger.info { "" }
        logger.info { "--- Writing Manifest... ---" }
        val manifest = Manifest(
            installerLocation = installerLocation,
            title = modpackTitle,
            version = uniqueVersion,
            id = modpackId,
            objectsLocation = objectsLocation,
            gameVersion = gameVersion,
            modLoader = modLoader,
            features = applicator.featuresInUse,
            tasks = tasks
        )
        manifest.validate()
        manifestDest.absoluteFile.parentFile.mkdirs()
        manifestDest.writeText(json.encodeToString(Manifest.serializer(), manifest))

        val versionsListing = if (versionlistingFile.exists()) {
            voodoo.util.json.decodeFromString(VersionsList.serializer(), versionlistingFile.readText())
        } else {
            VersionsList(
                installerLocation = installerLocation,
                versions = mapOf()
            )
        }

        val versionEntry = VersionEntry(
            version = modpackVersion,
            title = "$modpackTitle $modpackVersion",
            location = manifestDest.toRelativeUnixPath(versionlistingFile.absoluteFile.parentFile)
        )

        //TODO: add updateChannels to version listing

        val newVersionListing = versionsListing.copy(
            versions = versionsListing.versions + (modpackVersion to versionEntry)
        )

        versionlistingFile.writeText(
            voodoo.util.json.encodeToString(
                VersionsList.serializer(),
                newVersionListing
            )
        )

        logger.info { "" }
        logger.info { "--- Done ---" }
        // done
        logger.info { "Now upload the contents of $outputPath to your web server or CDN!" }
    }

    fun addFiles(
        filesDir: File,
        destDir: File,
        onEntry: (FileInstall) -> Unit
    ): List<FileInstall> {
        logger.info { "" }
        logger.info { "--- Adding files to modpack... ---" }
        val entries = filesDir.walkBottomUp().filterNot { file ->
            // conditions to skip files / folders
            file.name.endsWith(INFO_FILE_SUFFIX)
                    || file.name.endsWith(URL_FILE_SUFFIX)
//                    || (file.isDirectory && (file.name == "_SERVER" || file.name == "_CLIENT"))
                    || file.name == ".DS_Store"
                    || !file.isFile
        }.map { file ->
            // when this is a special file, skip
//            if (file.name.endsWith(INFO_FILE_SUFFIX) || file.name.endsWith(URL_FILE_SUFFIX)) {
//                return@mapNotNull null
//            }
//            // when this is a side marker, skip
//            if (file.isDirectory && (file.name == "_SERVER" || file.name == "_CLIENT")) {
//                return@mapNotNull null
//            }
            val side = run {
                val relativePath = file.relativeTo(filesDir).toPath()
                val markerCount = relativePath.count {
                    it.fileName.toString() == "_SERVER" || it.fileName.toString() == "_CLIENT"
                }
                require(markerCount <= 1) {
                    "found multiple markers in path: $relativePath"
                }

                val isServer = relativePath.count {
                    it.fileName.toString() == "_SERVER"
                } > 0
                val isClient = relativePath.count {
                    it.fileName.toString() == "_CLIENT"
                } > 0
                require(!(isServer && isClient)) { "cannot have both side markers at once: $relativePath" }

                when {
                    isServer -> Side.SERVER
                    isClient -> Side.CLIENT
                    else -> Side.BOTH
                }
            }

            val relPath = file.relativeTo(filesDir).toPath().filterNot {
                it.fileName.toString() == "_SERVER" || it.fileName.toString() == "_CLIENT"
            }.reduce { acc, path ->
                acc.resolve(path)
            }.toFile()

            val sha2 = MessageDigest.getInstance("SHA-256")
            val hash = sha2.digest(file.readBytes()).toHexString()
            val to = relPath.toString().replace('\\', '/')
            val urlFile = file.parentFile.resolve(file.name + URL_FILE_SUFFIX)
            val (location, copy) = if (urlFile.exists()) {
                // TODO: validate urlFile by downloading ?
                urlFile.readLines().first() to false
            } else {
                (hash.substring(0, 8) + "/" + hash.substring(8, 16) + "/" + hash.substring(16)) to true
            }
            val destPath = File(destDir, location)
            val entry = FileInstall(
                hash = "sha-256:$hash",
                location = location,
                to = to,
                side = side,
                size = file.length()
            )

            // TODO: applicator.apply(entry)
            // applicator is populated by scan
            // entry.conditionWhen = fromFeature(path)
            // entry.isUserFile = isUserFile(path)
            onEntry.invoke(entry)

            destPath.parentFile.mkdirs()
            if (copy) {
                file.copyTo(destPath, overwrite = true)
            }
            entry
        }
        return entries.toList()
    }

    const val DEFAULT_SRC_DIRNAME = "src"
    const val URL_FILE_SUFFIX = ".url.txt"
    const val INFO_FILE_SUFFIX = ".info.json"


    fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })
}