package voodoo.mmc.data

import kotlinx.serialization.Serializable

@Serializable
data class PackComponent(
    var uid: String = "",
    var version: String = "",
    var cachedName: String = "",
    var cachedRequires: List<CachedRequire> = listOf(),
    var cachedVersion: String = "",
    var important: Boolean = false,
    var cachedVolatile: Boolean = false,
    var dependencyOnly: Boolean = false
)
//{
//    @Serializer(forClass = PackComponent::class)
//    companion object : KSerializer<PackComponent> {
//        private val DEFAULT = PackComponent()
//        override fun serialize(encoder: Encoder, obj: PackComponent) {
//            val elemOutput = encoder.beginStructure(descriptor)
//            elemOutput.serialize(DEFAULT.uid, obj.uid, 0)
//            elemOutput.serialize(DEFAULT.version, obj.version, 1)
//            elemOutput.serialize(DEFAULT.cachedName, obj.cachedName, 2)
//            if (DEFAULT.cachedRequires != obj.cachedRequires) {
//                val listSerializer = ListSerializer(CachedRequire)
//                elemOutput.encodeSerializableElement(descriptor, 3, listSerializer, obj.cachedRequires)
//            }
//            elemOutput.serialize(DEFAULT.cachedVersion, obj.cachedVersion, 4)
//            elemOutput.serialize(DEFAULT.important, obj.important, 5)
//            elemOutput.serialize(DEFAULT.cachedVolatile, obj.cachedVolatile, 6)
//            elemOutput.serialize(DEFAULT.dependencyOnly, obj.dependencyOnly, 7)
//            elemOutput.endStructure(descriptor)
//        }
//
//        private inline fun <reified T : Any> CompositeEncoder.serialize(default: T, actual: T, index: Int) {
//            if (default != actual) {
//                when (actual) {
//                    is String -> this.encodeStringElement(descriptor, index, actual)
//                    is Int -> this.encodeIntElement(descriptor, index, actual)
//                    is Boolean -> this.encodeBooleanElement(descriptor, index, actual)
//                }
//            }
//        }
//    }
//}