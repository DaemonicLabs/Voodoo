package newformat.modpack

import kotlinx.serialization.Serializable
import newformat.modpack.entry.FileInstall
import Modloader

@Serializable
data class Manifest(

    // what is this even ?
    //var minimumVersion: Int = 0,

    var id: String,

    var title: String,

    var version: String,
//    @Serializable(with = URLSerializer::class)
//    var baseUrl: URL? = null,

//    var librariesLocation: String? = null,

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

    //    @Serializer(forClass = Manifest::class)
//    companion object : KSerializer<Manifest> {
    companion object {
//
//        override fun serialize(output: Encoder, obj: Manifest) {
//            val elemOutput = output.beginStructure(descriptor)
//            obj.title?.let { title ->
//                elemOutput.encodeStringElement(descriptor, 1, title)
//            }
//            obj.displayName?.let { displayName ->
//                elemOutput.encodeStringElement(descriptor, 2, displayName)
//            }
//            obj.version?.let { version ->
//                elemOutput.encodeStringElement(descriptor, 3, version)
//            }
//            elemOutput.encodeIntElement(descriptor, 0, obj.minimumVersion)
//            obj.baseUrl?.let { baseUrl ->
//                elemOutput.encodeStringElement(descriptor, 4, baseUrl.toString())
//            }
//            obj.librariesLocation?.let { librariesLocation ->
//                elemOutput.encodeStringElement(descriptor, 5, librariesLocation)
//            }
//            obj.objectsLocation?.let { objectsLocation ->
//                elemOutput.encodeStringElement(descriptor, 6, objectsLocation)
//            }
//            obj.gameVersion?.let { gameVersion ->
//                elemOutput.encodeStringElement(descriptor, 7, gameVersion)
//            }
//            obj.launchModifier?.let { launchModifier ->
//                elemOutput.encodeSerializableElement(descriptor, 8, LaunchModifier.serializer(), launchModifier)
//            }
//            obj.features.takeUnless { it.isEmpty() }?.let { features ->
//                elemOutput.encodeSerializableElement(descriptor, 9, Feature.serializer().list, features)
//            }
//            obj.tasks.takeUnless { it.isEmpty() }?.let { tasks ->
//                elemOutput.encodeSerializableElement(descriptor, 10, FileInstall.serializer().list, tasks)
//            }
//            obj.versionManifest?.let { versionManifest ->
//                elemOutput.encodeSerializableElement(
//                    descriptor,
//                    11,
//                    VersionManifest.serializer(),
//                    versionManifest
//                )
//            }
//            elemOutput.endStructure(descriptor)
//        }
    }
}
