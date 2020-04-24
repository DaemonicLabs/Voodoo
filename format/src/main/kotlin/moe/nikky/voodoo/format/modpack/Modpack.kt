package moe.nikky.voodoo.format.modpack


import kotlinx.serialization.Serializable
import moe.nikky.voodoo.format.FeatureWithPattern
import moe.nikky.voodoo.format.FnPatternList

@Serializable
data class Modpack(
    var name: String,
    var title: String = "",
    var gameVersion: String,

    var features: List<FeatureWithPattern> = listOf(),

    // TODO: look into this: icon for multimc and modpack ?
    var thumb: String? = null,

    //TODO: use this to setup server.dat in the future ?
//    var server: Server? = null,

    var userFiles: FnPatternList = FnPatternList()
)