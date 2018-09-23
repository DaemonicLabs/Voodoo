// Generated by delombok at Sat Jul 14 01:46:55 CEST 2018
/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher.builder

import com.skcraft.launcher.LauncherUtils
import com.skcraft.launcher.model.loader.InstallProfile
import com.skcraft.launcher.model.minecraft.Library
import com.skcraft.launcher.model.minecraft.VersionManifest
import com.skcraft.launcher.model.modpack.Manifest
import com.skcraft.launcher.util.Environment
import com.xenomachina.argparser.ArgParser
import io.ktor.client.request.get
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.io.IOException
import kotlinx.io.InputStream
import mu.KLogging
import voodoo.util.Directories
import voodoo.util.copyInputStreamToFile
import voodoo.util.download
import kotlinx.io.core.Closeable
import kotlinx.serialization.SerialContext
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import voodoo.util.Downloader.client
import java.io.File
import java.util.Properties
import java.util.jar.JarFile
import java.util.regex.Pattern

/**
 * Builds packages for the launcher.
 */
class PackageBuilder
/**
 * Create a new package builder.
 *
 * @param mapper the mapper
 * @param manifest the manifest
 */
@Throws(IOException::class)
constructor(
    private val manifest: Manifest,
    val isPrettyPrint: Boolean
) {
    private val properties: Properties = LauncherUtils.loadProperties(
        LauncherUtils::class.java,
        "launcher.properties",
        "com.skcraft.launcher.propertiesFile"
    )
    private val json: JSON = if (isPrettyPrint) {
        JSON(indented = true, nonstrict = true, context = SerialContext())
    } else {
        JSON(nonstrict = true, context = SerialContext())
    }
    private val applicator: PropertiesApplicator = PropertiesApplicator(manifest)
    private val loaderLibraries = arrayListOf<Library>()
    private var mavenRepos: List<String>? = null

    init {
        mavenRepos = LauncherUtils::class.java.getResourceAsStream("maven_repos.json").use {
            json.parse<List<String>>(String.serializer().list, it.bufferedReader().use { reader -> reader.readText() })
//            mapper.readValue<List<String>>(
//                it,
//                object : TypeReference<List<String>>() {}
//            )
        }
    }

    @Throws(IOException::class)
    fun scan(dir: File?) {
        logSection("Scanning for .info.json files...")
        val scanner = FileInfoScanner()
        scanner.walk(dir!!)
        for (pattern in scanner.patterns) {
            applicator.register(pattern)
        }
    }

    @Throws(IOException::class)
    fun addFiles(dir: File?, destDir: File?) {
        logSection("Adding files to modpack...")
        val collector = ClientFileCollector(this.manifest, applicator, destDir!!)
        collector.walk(dir!!)
    }

    fun addLoaders(dir: File?, librariesDir: File?) {
        logSection("Checking for mod loaders to install...")
        val collected = LinkedHashSet<Library>()
        val files = dir!!.listFiles(JarFileFilter())
        if (files != null) {
            for (file in files) {
                try {
                    processLoader(collected, file, librariesDir)
                } catch (e: IOException) {
                    logger.warn("Failed to add the loader at ${file.absolutePath}", e)
                }
            }
        }
        this.loaderLibraries.addAll(collected)
        val version = manifest.versionManifest!!
        collected.addAll(version.libraries)
        version.libraries = collected
    }

    @Throws(IOException::class)
    private fun processLoader(loaderLibraries: LinkedHashSet<Library>, file: File, librariesDir: File?) {
        logger.info("Installing ${file.name}...")
        JarFile(file).use { jarFile ->
            val profileEntry = BuilderUtils.getZipEntry(jarFile, "install_profile.json")
            if (profileEntry != null) {
                // Read file
                var data = jarFile.getInputStream(profileEntry).bufferedReader().use { it.readText() }
                data = data.replace(",\\s*\\}".toRegex(), "}") // Fix issues with trailing commas
                val profile: InstallProfile = json.parse(data)
                //mapper.readValue<InstallProfile>(data, InstallProfile::class.java)
                val version = manifest.versionManifest
                // Copy tweak class arguments
                val args = profile.versionInfo.minecraftArguments
                val existingArgs = version?.minecraftArguments ?: ""
                val m = TWEAK_CLASS_ARG.matcher(args)
                while (m.find()) {
                    version?.minecraftArguments = existingArgs + " " + m.group()
                    logger.info("Adding ${m.group()} to launch arguments")
                }

                // Add libraries
                val libraries = profile.versionInfo.libraries
                for (library in libraries) {
                    if (version?.libraries?.contains(library) != true) {
                        loaderLibraries.add(library)
                    }
                }

                // Copy main class
                val mainClass = profile.versionInfo.mainClass
                version?.mainClass = mainClass
                logger.info("Using $mainClass as the main class")

                // Extract the library
                val filePath = profile.installData.filePath
                val libraryPath = profile.installData.path
                val libraryEntry = BuilderUtils.getZipEntry(jarFile, filePath)
                if (libraryEntry != null) {
                    val library = Library(name = libraryPath)
                    val extractPath = File(librariesDir, library.getPath(Environment.instance))
                    extractPath.parentFile.mkdirs()
                    jarFile.getInputStream(libraryEntry).use { input ->
                        extractPath.copyInputStreamToFile(input)
                    }
                } else {
                    logger.warn("Could not find the file '$filePath' in ${file.absolutePath}, which means that this mod loader will not work correctly")
                }
            } else {
                logger.warn("The file at ${file.absolutePath} did not appear to have an install_profile.json file inside -- is it actually an installer for a mod loader?")
            }
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    fun downloadLibraries(librariesDir: File?) {
        logSection("Downloading libraries...")
        // TODO: Download libraries for different environments -- As of writing, this is not an issue

        val directories = Directories.get(moduleName = "sklauncher")
        val cache = directories.cacheHome
        val pool = newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors() + 1, "pool")

        val env = Environment.instance
        runBlocking {
            val jobs = mutableListOf<Job>()
            for (library in loaderLibraries) {
                jobs += launch(context = pool) {
                    val outputPath = File(librariesDir, library.getPath(env))
                    if (!outputPath.exists()) {
                        outputPath.parentFile.mkdirs()
                        var found = false
                        // Gather a list of repositories to download from
                        val sources = arrayListOf<String>() //Lists.newArrayList<String>()
                        library.baseUrl?.let {
                            sources.add(it)
                        }
                        sources.addAll(mavenRepos!!)
                        // Try each repository
                        for (baseUrl in sources) {
                            var pathname = library.getPath(env)
                            // Some repositories compress their files
                            val compressors = BuilderUtils.getCompressors(baseUrl)
                            for (compressor in compressors.reversed()) {
                                pathname = compressor.transformPathname(pathname)
                            }
                            val url = baseUrl + pathname
                            val tempFile = File.createTempFile("launcherlib", null)
                            try {
                                logger.info("Downloading library " + library.name + " from " + url + "...")
                                tempFile.download(url, cache)
//                        HttpRequest.get(URL(url)).execute().expectResponseCode(200).saveContent(tempFile)
//                            val (_, response, result) = url.httpGet().response()
//                            when (result) {
//                                is Result.Success -> {
//                                    logger.info("writing to $tempFile")
//                                    tempFile.writeBytes(result.value)
//                                }
//                                is Result.Failure -> {
//                                    throw IOException("Did not get expected response code, got ${response.statusCode} for $url")
//                                }
//                            }
                            } catch (e: IOException) {
                                logger.info("Could not get file from " + url + ": " + e.message)
                                continue
                            }

                            logger.info("downloaded to $tempFile")
                            // Decompress (if needed) and write to file
                            val closeables = mutableListOf<Closeable>()
                            tempFile.inputStream().use { inputStream: InputStream ->
                                closeables += inputStream
                                val input = compressors.fold(inputStream) { input, compressor ->
                                    compressor.createInputStream(input).also { closeables += it }
                                }
                                outputPath.copyInputStreamToFile(input)
                                closeables.forEach { it.close() }
                            }
                            tempFile.delete()
                            found = true
                            break
                        }
                        if (!found) {
                            logger.warn("!! Failed to download the library " + library.name + " -- this means your copy of the libraries will lack this file")
                        }
                    }
                }
            }

            logger.info("waiting for library jobs to finish")
            jobs.forEach { it.join() }
        }
    }

    private fun validateManifest() {
        if (manifest.name.isNullOrEmpty()) {
            throw IllegalStateException("Package name is not defined")
        }
        if (manifest.gameVersion.isNullOrEmpty()) {
            throw IllegalStateException("Game version is not defined")
        }
    }

    @Throws(IOException::class)
    fun readConfig(path: File?) {
        if (path != null) {
            val config = read<BuilderConfig>(path)
            config.update(manifest)
            config.registerProperties(applicator)
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private suspend fun readVersionManifest(path: File?) {
        logSection("Reading version manifest...")
        if (path!!.exists()) {
            val versionManifest = read<VersionManifest>(path)
            manifest.versionManifest = versionManifest
            logger.info("Loaded version manifest from " + path.absolutePath)
        } else {
            val url = String.format(properties.getProperty("versionManifestUrl"), manifest.gameVersion)
            logger.info("Fetching version manifest from $url...")
           try {
               manifest.versionManifest = client.get(url)
           } catch(e: Exception) {
               logger.error("cannot parse manifest from $url", e)
               throw e
           }
        }
    }

    @Throws(IOException::class)
    fun writeManifest(path: File) {
        logSection("Writing manifest...")
        manifest.features = applicator.featuresInUse
        val versionManifest = manifest.versionManifest
        if (versionManifest != null) {
            versionManifest.id = manifest.gameVersion
        }
        validateManifest()
        path.absoluteFile.parentFile.mkdirs()
        path.writeText(json.stringify(manifest))
//        json!!.writeValue(path, manifest)
        logger.info("Wrote manifest to " + path.absolutePath)
    }

    @Throws(IOException::class)
    private inline fun <reified V : Any> read(path: File?): V {
        try {
            return if (path == null) {
                V::class.java.newInstance()
            } else {
                json.parse(path.readText())
//                mapper.readValue(path, V::class.java)
            }
        } catch (e: InstantiationException) {
            throw IOException("Failed to create " + V::class.java.canonicalName, e)
        } catch (e: IllegalAccessException) {
            throw IOException("Failed to create " + V::class.java.canonicalName, e)
        }
    }

    companion object : KLogging() {
        private val TWEAK_CLASS_ARG = Pattern.compile("--tweakClass\\s+([^\\s]+)")

        /**
         * Build a package given the arguments.
         *
         * @param args arguments
         * @throws IOException thrown on I/O error
         * @throws InterruptedException on interruption
         */
//        @Throws(IOException::class, InterruptedException::class)
        suspend fun main(vararg args: String) {
            val parser = ArgParser(args)
            val options = BuilderOptions(parser)
            parser.force()

            // Initialize
            val manifest = Manifest(
                minimumVersion = Manifest.MIN_PROTOCOL_VERSION
            )
            val builder = PackageBuilder(
                manifest = manifest,
                isPrettyPrint = options.isPrettyPrinting
            )
            // From config
            builder.readConfig(options.configPath)
            builder.readVersionManifest(options.versionManifestPath)
            // From options
            manifest.updateName(options.name)
            manifest.updateTitle(options.title)
            manifest.updateGameVersion(options.gameVersion)
            manifest.version = options.version
            manifest.librariesLocation = options.librariesLocation
            manifest.objectsLocation = options.objectsLocation
            builder.scan(options.filesDir)
            builder.addFiles(options.filesDir, options.objectsDir)
            builder.addLoaders(options.loadersDir, options.librariesDir)
            builder.downloadLibraries(options.librariesDir)
            builder.writeManifest(options.manifestPath)
            logSection("Done")
            logger.info("Now upload the contents of " + options.outputPath + " to your web server or CDN!")
        }

        private fun logSection(name: String) {
            logger.info("")
            logger.info("--- $name ---")
        }
    }
}
