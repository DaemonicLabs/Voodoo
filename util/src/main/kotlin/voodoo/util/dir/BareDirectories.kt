package voodoo.util.dir

import voodoo.util.Directories
import voodoo.util.Directories.Companion.deleteDirectoryOnExit
import java.io.File

class BareDirectories(private val appName: String) : Directories {
    private var hasUsedRuntimeDir = false

    override val dataHome: File  by lazy {
        File(home, "data")
                .apply { mkdirs() }
    }

    override val configHome: File by lazy {
        File(home, "config")
                .apply { mkdirs() }
    }

    override val cacheHome: File by lazy {
        File(home, "cache")
                .apply { mkdirs() }
    }

    override val pluginHome: File by lazy {
        File(home, "plugins")
                .apply { mkdirs() }
    }

    override val runtimeDir: File by lazy {
        File(home, "runtime")
                .apply {
                    if (!hasUsedRuntimeDir) {
                        hasUsedRuntimeDir = true
                        deleteDirectoryOnExit()
                    }
                    mkdirs()
                }
    }

    private val home: File by lazy {
        File(System.getProperty("user.home"), ".$appName")
    }

}
