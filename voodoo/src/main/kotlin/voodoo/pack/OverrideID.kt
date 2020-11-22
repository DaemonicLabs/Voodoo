package voodoo.pack

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = OverrideID.Companion::class)
data class OverrideID (val value: String) {
    @Serializer(forClass = OverrideID::class)
    companion object : KSerializer<OverrideID> {
        override val descriptor = PrimitiveSerialDescriptor("OverrideID", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): OverrideID {
            return OverrideID(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, value: OverrideID) {
            encoder.encodeString(value.value)
        }
    }
}