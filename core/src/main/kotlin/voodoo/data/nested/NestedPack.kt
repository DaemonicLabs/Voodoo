package voodoo.data.nested

import kotlinx.serialization.Serializable
import mu.KLogging
import voodoo.data.ModloaderPattern
import voodoo.data.PackOptions
import voodoo.data.flat.ModPack
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */
@Serializable
data class NestedPack(
    val `$schema`: String = "./schema/nested.schema.json",
    /**
     * Minecraft Version
     */
    var mcVersion: String? = null,
    /**
     * display name
     */
    var title: String? = null,
    var version: String = "1.0",
    var icon: String = "icon.png",
    var authors: List<String> = emptyList(),
    var modloader: ModloaderPattern = ModloaderPattern.None,
    var localDir: String = "local",
    var docDir: String? = null,
    var packOptions: PackOptions = PackOptions(),
    var root: NestedEntry = NestedEntry.Common(
        nodeName = "root"
    )
) {
    companion object : KLogging() {
        fun create(builder: (NestedPack) -> Unit = {}): NestedPack {
            val pack = NestedPack()
            builder(pack)
            return pack
        }
    }

    // TODO: possibly this flattening step will not be necessary
    suspend fun flatten(rootFolder: File, id: String): ModPack {
        if (!rootFolder.isAbsolute) {
            throw IllegalStateException("rootFolder: '$rootFolder' is not absolute")
        }
        return ModPack(
            rootFolder = rootFolder,
            id = id,
            mcVersion = mcVersion ?: throw IllegalStateException("mcVersion must be set for pack '$id'"),
            title = title,
            version = version,
            icon = rootFolder.resolve(icon),
            authors = authors,
            modloader = modloader,
            localDir = localDir,
            docDir = docDir ?: id,
            packOptions = packOptions
        ).also {
            it.entrySet += root.flatten()
        }
    }
}