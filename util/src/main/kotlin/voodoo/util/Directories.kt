package voodoo.util

import mu.KLogging
import voodoo.util.dir.BareDirectories
import voodoo.util.dir.MacDirectories
import voodoo.util.dir.WindowsDirectories
import voodoo.util.dir.XDGDirectories
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.*


interface Directories {
    /**
     * @return The OS-specific single base directory relative to which
     * user-specific data files should be written.
     */
    val dataHome: File

    /**
     * @return The OS-specific single base directory relative to which
     * user-specific configuration files should be written.
     */
    val configHome: File

    /**
     * @return The OS-specific single base directory relative to which
     * user-specific non-essential (cached) data should be written.
     */
    val cacheHome: File

    /**
     * @return The OS-specific single base directory relative to which
     * plugins will be found.
     */
    val pluginHome: File

    /**
     * @return The OS-specific single base directory relative to which
     * user-specific non-essential runtime files and other file objects
     * (such as sockets, named pipes, ...) should be stored.
     */
    val runtimeDir: File

    companion object : KLogging() {

        fun File.deleteDirectoryOnExit() {
            Runtime.getRuntime().addShutdownHook(Thread({
                try {
                    Files.walkFileTree(toPath(), object : SimpleFileVisitor<Path>() {
                        @Throws(IOException::class)
                        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                            Files.delete(file)
                            return FileVisitResult.CONTINUE
                        }

                        @Throws(IOException::class)
                        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                            Files.delete(dir)
                            return FileVisitResult.CONTINUE
                        }
                    })
                } catch (e: IOException) {
                    logger.warn("Failed to delete {}", this, e)
                }
            }, "Runtime directory cleanup thread"))
        }

        fun get(appName: String = "voodoo", moduleName: String? = null, useBareDirectories: Boolean = false): Directories {
            var cleanAppName = appName.toLowerCase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "-")
            if(moduleName != null) {
                val cleanModuleName = moduleName.toLowerCase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "-")
                cleanAppName += "/" + cleanModuleName
            }
            val directories: Directories

            directories = when {
                useBareDirectories -> {
                    logger.info("Using bare directories, as requested")
                    BareDirectories(cleanAppName)
                }
                Platform.isLinux || Platform.isX11 || Platform.isSolaris || Platform.isAIX -> {
                    logger.info("Using XDG directories")
                    XDGDirectories(cleanAppName)
                }
                Platform.isMac -> {
                    logger.info("Using Mac Library directories")
                    MacDirectories(appName.replace('/', '_'));
                }
                Platform.isWindows -> {
                    logger.info("Using Windows directories")
                    WindowsDirectories(appName.replace('/', '_').replace(" ", ""))
                }
                else -> {
                    logger.info("Using bare directories")
                    BareDirectories(cleanAppName)
                }

            }
            return directories
        }
    }
}