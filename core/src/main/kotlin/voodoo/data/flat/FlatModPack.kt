package voodoo.data.flat

import com.eyeem.watchadoin.Stopwatch
import kotlinx.serialization.Transient
import mu.KLogging
import mu.KotlinLogging
import voodoo.builder.Builder
import voodoo.builder.resolve
import voodoo.data.ModloaderPattern
import voodoo.data.PackOptions
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.provider.Providers
import voodoo.util.toRelativeUnixPath
import java.io.File
import java.util.Collections
import voodoo.util.unixPath
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

//@Serializable
data class FlatModPack(
//    @Serializable(with = FileSerializer::class)
    var rootFolder: File,
    /**
     * unique identifier
     */
    var id: String,
    /**
     * Minecraft Version
     */
    var mcVersion: String,
    var title: String? = null,
    var version: String = "1.0",
//    @Serializable(with = FileSerializer::class)
    var srcDir: String = "src",
    var icon: String = "icon.png",
    val authors: List<String> = emptyList(),
    var modloader: ModloaderPattern? = null,
    var localDir: String = "local",
    var docDir: String = id,
    var packOptions: PackOptions = PackOptions(),
    // we want this to be serialized for debugging purposes ?
    val entrySet: Set<FlatEntry> = setOf(),
) {
    companion object {
        private val logger = KotlinLogging.logger {  }
        fun srcFolder(baseFolder: File): File {
            return baseFolder.resolve("src")
        }
    }

    @Transient
    val baseFolder: File
        get() = rootFolder.resolve(id)
    @Transient
    val sourceFolder: File
        get() = baseFolder.resolve(srcDir)
    @Transient
    val localFolder: File
        get() = rootFolder.resolve(localDir)
    @Transient
    val iconFile: File
        get() = baseFolder.resolve(icon)

    suspend fun lock(stopwatch: Stopwatch, targetFolder: File): LockPack = stopwatch {
        val resolvedEntries = resolve(
            "resolve".watch,
            this@FlatModPack
        )

        resolvedEntries.forEach { entry ->
            logger.info { "RESOLVED: ${entry.id} $entry" }
        }

        "validate".watch {
            resolvedEntries.forEach { lockEntry ->
                val provider = Providers[lockEntry.providerType]
                if (!provider.validate(lockEntry)) {
                    Builder.logger.error { lockEntry }
                    throw IllegalStateException("entry '${lockEntry.id}' did not validate")
                }
            }
        }

        "creating Lockpack".watch {
            LockPack(
                id = id,
//                srcPath = "src", //sourceFolder.toRelativeUnixPath(baseFolder),
                title = title,
                version = version,
                icon = iconFile.absoluteFile.toRelativeUnixPath(baseFolder),
                authors = authors,
                mcVersion = mcVersion,
                modloader = modloader?.lock() ?: Modloader.None,
                localDir = localDir,
                packOptions = packOptions,
                entries = resolvedEntries.sortedBy { it.id }
            ).also {
                it.lockBaseFolder = targetFolder
            }
        }
    }
}
