package voodoo.core.data.nested

import voodoo.core.data.flat.Entry
import voodoo.core.data.flat.ModPack

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 * @version 1.0
 */
data class NestedPack(
        var name: String,
        var title: String = "",
        var version: String = "1.0",
        var forge: Int = -1,
        var mcVersion: String = "",
        var root: NestedEntry = NestedEntry(),
        var versionCache: String? = null,
        var featureCache: String? = null
) {
    fun flatten(): ModPack {
        return ModPack(
                name = name,
                title = title,
                version = version,
                forge = forge,
                mcVersion = mcVersion,
                entries = root.flatten(),
                versionCache = versionCache,
                featureCache = featureCache
        )
    }
}