package voodoo.util.dir

import com.sun.jna.platform.win32.KnownFolders
import com.sun.jna.platform.win32.Shell32Util
import voodoo.util.Directories

import java.io.File

class WindowsDirectories(private val appName: String) : Directories {
    private var hasUsedRuntimeDir = false

    override val dataHome: File by lazy {
        File(home, "Data")
                .apply { mkdirs() }
    }

    override val configHome: File by lazy {
        File(home, "Config")
                .apply { mkdirs() }
    }

    override val cacheHome: File by lazy {
        File(home, "Cache")
                .apply { mkdirs() }
    }

    override val pluginHome: File by lazy {
        File(home, "Plugins")
                .apply { mkdirs() }
    }

    override val runtimeDir: File by lazy {
        File(home, "Runtime")
                .apply {
                    if (!hasUsedRuntimeDir) {
                        hasUsedRuntimeDir = true
//                        Directories.deleteOnExit(f)
                        deleteOnExit()
                    }
                    mkdirs()
                }
    }

    private val home: File by lazy {
        File(Shell32Util.getKnownFolderPath(KnownFolders.FOLDERID_RoamingAppData), appName)
    }

}
