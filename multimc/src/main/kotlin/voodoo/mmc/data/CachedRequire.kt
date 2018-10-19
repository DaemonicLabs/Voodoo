package voodoo.mmc.data

import kotlinx.serialization.CompositeEncoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

@Serializable(with = CachedRequire.Companion::class)
data class CachedRequire(
    @Optional var uid: String = "",
    @Optional var suggests: String = "",
    @Optional var equals: String = ""
) {
    @Serializer(forClass = CachedRequire::class)
    companion object : KSerializer<CachedRequire> {
        private val DEFAULT = CachedRequire()
        override fun serialize(output: Encoder, obj: CachedRequire) {
            val elemOutput = output.beginStructure(descriptor)
            elemOutput.serialize(DEFAULT.uid, obj.uid, 0)
            elemOutput.serialize(DEFAULT.suggests, obj.suggests, 1)
            elemOutput.serialize(DEFAULT.equals, obj.equals, 2)
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