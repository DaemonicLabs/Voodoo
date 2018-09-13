package voodoo.data.nested

import mu.KLogging
import voodoo.data.UserFiles
import voodoo.data.flat.ModPack
import voodoo.data.sk.Launch

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
         * display name
         */
        var title: String = "",
        var version: String = "1.0",
        var icon: String = "icon.png",
        val authors: List<String> = emptyList(),
        var forge: String = "recommended",
        var mcVersion: String = "",
        var userFiles: UserFiles = UserFiles(),
        var launch: Launch = Launch(),
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