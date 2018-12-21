package voodoo.mmc.data

import kotlinx.serialization.CompositeEncoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.list

@Serializable(with = PackComponent.Companion::class)
data class PackComponent(
    @Optional var uid: String = "",
    @Optional var version: String = "",
    @Optional var cachedName: String = "",
    @Optional var cachedRequires: List<CachedRequire> = listOf(),
    @Optional var cachedVersion: String = "",
    @Optional var important: Boolean = false,
    @Optional var cachedVolatile: Boolean = false,
    @Optional var dependencyOnly: Boolean = false
) {
    @Serializer(forClass = PackComponent::class)
    companion object : KSerializer<PackComponent> {
        private val DEFAULT = PackComponent()
        override fun serialize(encoder: Encoder, obj: PackComponent) {
            val elemOutput = encoder.beginStructure(descriptor)
            elemOutput.serialize(DEFAULT.uid, obj.uid, 0)
            elemOutput.serialize(DEFAULT.version, obj.version, 1)
            elemOutput.serialize(DEFAULT.cachedName, obj.cachedName, 2)
            if (DEFAULT.cachedRequires != obj.cachedRequires) {
                val listSerializer = CachedRequire.list
                elemOutput.encodeSerializableElement(descriptor, 3, listSerializer, obj.cachedRequires)
            }
            elemOutput.serialize(DEFAULT.cachedVersion, obj.cachedVersion, 4)
            elemOutput.serialize(DEFAULT.important, obj.important, 5)
            elemOutput.serialize(DEFAULT.cachedVolatile, obj.cachedVolatile, 6)
            elemOutput.serialize(DEFAULT.dependencyOnly, obj.dependencyOnly, 7)
            elemOutput.endStructure(descriptor)
        }

        private inline fun <reified T : Any> CompositeEncoder.serialize(default: T, actual: T, index: Int) {
            if (default != actual) {
                when (actual) {
                    is String -> this.encodeStringElement(descriptor, index, actual)
                    is Int -> this.encodeIntElement(descriptor, index, actual)
                    is Boolean -> this.encodeBooleanElement(descriptor, index, actual)
                }
            }
        }
    }
}