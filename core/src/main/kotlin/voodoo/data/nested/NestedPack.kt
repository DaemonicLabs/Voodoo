package voodoo.data.nested

import com.skcraft.launcher.model.launcher.LaunchModifier
import kotlinx.serialization.Transient
import mu.KLogging
import voodoo.data.PackOptions
import voodoo.data.flat.ModPack
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */
data class NestedPack
internal constructor(
    val rootDir: File,
    /**
     * unique identifier
     */
    var id: String,
    /**
     * Minecraft Version
     */
    var mcVersion: String? = null,
    /**
     * display name
     */
    var title: String? = null,
    var version: String = "1.0",
    var icon: File = rootDir.resolve("icon.png"),
    var authors: List<String> = emptyList(),
    var forge: String? = null, // TODO: replace with generic modloader info
    var launch: LaunchModifier = LaunchModifier(),
    var localDir: String = "local",
    var sourceDir: String = id,
    var docDir: String = id,
    var packOptions: PackOptions = PackOptions(),
    var root: NestedEntry = NestedEntry.Common()
) {
    companion object : KLogging() {
        fun create(rootDir: File, id: String, builder: (NestedPack) -> Unit = {}): NestedPack {
            val pack = NestedPack(
                rootDir = rootDir,
                id = id
            )
            builder(pack)
            return pack
        }
    }

    init {
        if (!rootDir.isAbsolute) {
            throw IllegalStateException("rootDir: '$rootDir' is not absolute")
        }
    }

    @Transient
    val sourceFolder: File
        get() = rootDir.resolve(sourceDir)
    @Transient
    val localFolder: File
        get() = rootDir.resolve(localDir)

    // TODO: possibly this flattening step will not be necessary
    suspend fun flatten(): ModPack {
        return ModPack(
            rootDir = rootDir,
            id = id,
            mcVersion = mcVersion ?: throw IllegalStateException("mcVersion must be set for pack '$id'"),
            title = title,
            version = version,
            icon = icon,
            authors = authors,
            forge = forge,
            launch = launch,
            localDir = localDir,
            sourceDir = sourceDir,
            docDir = docDir,
            packOptions = packOptions
        ).also {
            it.entrySet += root.flatten()
        }
    }
}