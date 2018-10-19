package voodoo.data.curse

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

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
//        override val descriptor = PrimitiveDesc("FileID")

        override fun deserialize(input: Decoder): FileID {
            return FileID(input.decodeInt())
        }

        override fun serialize(output: Encoder, obj: FileID) {
            output.encodeInt(obj.value)
        }

        val INVALID = FileID(-1)
    }
}