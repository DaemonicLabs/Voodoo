package voodoo.data.curse

import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor

enum class FileType {
    Release,
    Beta,
    Alpha;

    @Serializer(forClass = FileType::class)
    companion object {
        override val descriptor = PrimitiveSerialDescriptor("FileType", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): FileType {
            return values()[decoder.decodeInt()-1]
        }

        override fun serialize(encoder: Encoder, obj: FileType) {
            encoder.encodeInt(obj.ordinal + 1)
        }
    }
}