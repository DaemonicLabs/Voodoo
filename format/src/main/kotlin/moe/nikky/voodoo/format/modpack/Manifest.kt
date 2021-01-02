package moe.nikky.voodoo.format.modpack

import Modloader
import kotlinx.serialization.Serializable
import moe.nikky.voodoo.format.Feature
import moe.nikky.voodoo.format.modpack.entry.FileInstall

@Serializable
data class Manifest(
    val installerLocation: String,
    // what is this even ?
    //var minimumVersion: Int = 0,

    var id: String,
    var title: String,
    var version: String,
    var objectsLocation: String,
    // no longer required / move to versionManifest ?
    var gameVersion: String,
    var modLoader: Modloader,

    var features: List<Feature> = emptyList(),

    /*
    files to install
     */
    var tasks: List<FileInstall> = emptyList()
) {
    fun validate() {
        require(id.isNotBlank()) {
            "package id cannot be empty or blank"
        }
        require(title.isNotBlank()) {
            "package title cannot be blank"
        }
    }
}
