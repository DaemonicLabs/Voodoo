package voodoo.data.nested

import mu.KLogging
import voodoo.data.UserFiles
import voodoo.data.flat.ModPack
import voodoo.provider.Provider
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */
data class NestedPack(
        /**
         * Name of the Pack
         */
        var id: String,
        var title: String = "",
        var version: String = "1.0",
        val authors: List<String> = emptyList(),
        var forge: String = "recommended",
        var mcVersion: String = "",
        var userFiles: UserFiles = UserFiles(),
        var root: NestedEntry = NestedEntry(Provider.CURSE.name),
        var versionCache: File = File(".voodoo/", id),
        var featureCache: File = File(".voodoo/", id),
        var localDir: String = "local",
        var sourceDir: String = "src"
) {
    companion object : KLogging()

    fun flatten(): ModPack {
        return ModPack(
                id = id,
                title = title,
                version = version,
                authors = authors,
                forge = forge,
                mcVersion = mcVersion,
                userFiles = userFiles,
                localDir = localDir,
                sourceDir = sourceDir
        )
    }
}