package voodoo.data.curse

import kotlinx.serialization.*

// TODO: inline
@Serializable(with = FileID.Companion::class)
data class FileID(val value: Int) {
    override fun toString(): String {
        return value.toString()
    }

    val valid: Boolean
        get() = value > 0

    @Serializer(forClass = FileID::class)
    companion object : KSerializer<FileID> {
        override val descriptor = PrimitiveDescriptor("FileID", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): FileID {
            return FileID(decoder.decodeInt())
        }

        override fun serialize(encoder: Encoder, obj: FileID) {
            encoder.encodeInt(obj.value)
        }

        val INVALID = FileID(-1)
    }
}