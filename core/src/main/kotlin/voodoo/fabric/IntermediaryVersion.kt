package voodoo.fabric

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.Serializer

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