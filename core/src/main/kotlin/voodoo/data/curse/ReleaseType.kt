package voodoo.data.curse

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.IntDescriptor

enum class ReleaseType {
    Release,
    Beta,
    Alpha;

    @Serializer(forClass = ReleaseType::class)
    companion object {
        override val descriptor: SerialDescriptor = IntDescriptor

        override fun deserialize(decoder: Decoder): ReleaseType {
            return values()[decoder.decodeInt()-1]
        }

        override fun serialize(encoder: Encoder, obj: ReleaseType) {
            encoder.encodeInt(obj.ordinal + 1)
        }
    }
}