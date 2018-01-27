package voodoo.util.dir


import voodoo.util.Directories
import java.io.File

/**
 * [https://developer.apple.com/library/content/qa/qa1170/_index.html](https://developer.apple.com/library/content/qa/qa1170/_index.html)
 */
class MacDirectories(private val appName: String) : Directories {

    override val runtimeDir: File by lazy {
        createTempDir().apply { deleteOnExit() }
    }

    override val dataHome: File by lazy {
        File(configHome, "Data")
                .apply { mkdirs() }
    }

    override val configHome: File by lazy {
        File(getLibrary("Preferences"), appName)
                .apply { mkdirs() }
    }

    override val cacheHome: File by lazy {
        File(getLibrary("Caches"), appName)
                .apply { mkdirs() }
    }

    override val pluginHome: File by lazy {
        File(getLibrary("Application Support"), appName)
                .apply { mkdirs() }
    }

    private fun getLibrary(base: String): File {
        return File(System.getProperty("user.home"), "Library/" + base)
    }
}
