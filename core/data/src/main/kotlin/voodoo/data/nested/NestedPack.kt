package voodoo.data.nested

import voodoo.data.UserFiles
import voodoo.flatten.data.NestedEntry
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */
data class NestedPack(
        var name: String,
        var title: String = "",
        var version: String = "1.0",
        val authors: List<String> = emptyList(),
        var forge: String = "recommended",
        var mcVersion: String = "",
        var userFiles: UserFiles = UserFiles(),
        var root: NestedEntry = NestedEntry(),
        var versionCache: File = File(".voodoo/", name),
        var featureCache: File = File(".voodoo/", name),
        var localDir: String = "local",
        var minecraftDir: String = name
)