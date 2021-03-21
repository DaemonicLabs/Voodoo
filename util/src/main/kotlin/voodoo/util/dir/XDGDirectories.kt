package voodoo.util.dir

import mu.KotlinLogging
import voodoo.util.Directories

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

/**
 * Basic implementation of the [XDG Base Directory Specification](https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html),
 * in Java (obviously).
 */
class XDGDirectories(private val appName: String) : Directories {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    /**
     * @return The single base directory relative to which user-specific
     * non-essential runtime files and other file objects (such as sockets,
     * named pipes, ...) should be stored.
     */
    override val runtimeDir: File by lazy {
        val dir = getBaseDir("XDG_RUNTIME_DIR") ?: run run@{
            logger.warn("Synthesizing runtime directory, as \$XDG_RUNTIME_DIR is unset")
            var dir = File(System.getProperty("java.io.tmpdir"))
            dir = File(dir, appName + "-" + System.getProperty("user.name"))
            dir.mkdirs()
            return@run dir
        }

        try {
            Files.setPosixFilePermissions(dir.toPath(), PosixFilePermissions.fromString("rwx------"))
        } catch (e: IOException) {
            logger.warn("Failed to set directory permissions on {} to owner-only", dir, e)
        } catch (e: UnsupportedOperationException) {
            logger.warn("Failed to set directory permissions on {} to owner-only", dir, e)
        }

//        Directories.deleteOnExit(dir)
        dir.deleteOnExit()
        dir
    }

    /**
     * @return The single base directory relative to which user-specific data
     * files should be written. This directory is defined by the
     * environment variable `$XDG_DATA_HOME`.
     */
    override val dataHome: File by lazy {
        getBaseDir("XDG_DATA_HOME", "/.local/share")
    }

    override val pluginHome: File by lazy {
        File(dataHome, "plugins")
            .apply { mkdirs() }
    }

    /**
     * @return The single base directory relative to which user-specific
     * configuration files should be written. This directory is defined by
     * the environment variable `$XDG_CONFIG_HOME`.
     */
    override val configHome: File by lazy {
        getBaseDir("XDG_CONFIG_HOME", "/.config")
    }

    /**
     * @return The single base directory relative to which user-specific
     * non-essential (cached) data should be written. This directory is defined
     * by the environment variable `$XDG_CACHE_HOME`.
     */
    override val cacheHome: File by lazy {
        getBaseDir("XDG_CACHE_HOME", "/.cache")
    }

    private fun getBaseDir(env: String, def: String): File {
        var home: String? = System.getenv("HOME")
        if (home == null || home.trim().isEmpty()) {
            home = System.getProperty("user.home")
        }
        var dir: String? = System.getenv(env)
        if (dir == null || dir.trim().isEmpty()) {
            dir = home!! + def
        }
        val f = File(dir, appName)
        f.mkdirs()
        return f
    }

    private fun getBaseDir(env: String): File? {
        val dir: String? = System.getenv(env)
        if (dir == null || dir.trim().isEmpty()) {
            return null
        }
        val f = File(dir, appName)
        f.mkdirs()
        return f
    }
}
