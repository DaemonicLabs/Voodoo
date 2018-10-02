package voodoo.data.nested

import com.skcraft.launcher.model.launcher.LaunchModifier
import mu.KLogging
import voodoo.data.UserFiles
import voodoo.data.flat.ModPack
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */
data class NestedPack(
    /**
     * unique identifier
     */
    var id: String,
    /**
     * Minecraft Version
     */
    var mcVersion: String,
    /**
     * display name
     */
    var title: String = "",
    var version: String = "1.0",
    var icon: File = File("icon.png"),
    val authors: List<String> = emptyList(),
    var forge: String = "recommended",
    var userFiles: UserFiles = UserFiles(),
    var launch: LaunchModifier = LaunchModifier(),
    var root: NestedEntry = NestedEntry(),
    var localDir: String = "local",
    var sourceDir: String = "src"
) {
    companion object : KLogging()

    fun flatten(): ModPack {
        return ModPack(
            id = id,
            title = title,
            version = version,
            icon = icon,
            authors = authors,
            forge = forge,
            mcVersion = mcVersion,
            userFiles = userFiles,
            launch = launch,
            localDir = localDir,
            sourceDir = sourceDir
        )
    }
}