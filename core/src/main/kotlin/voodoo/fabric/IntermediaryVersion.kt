package voodoo.fabric

import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

inline class IntermediaryVersion (
    val version: String
) {
    @Serializer(forClass = IntermediaryVersion::class)
    companion object {
        override fun serialize(encoder: Encoder, value: IntermediaryVersion) {
            encoder.encodeString(value.version)
        }

        override fun deserialize(decoder: Decoder): IntermediaryVersion {
            return IntermediaryVersion(decoder.decodeString())
        }
    }
}