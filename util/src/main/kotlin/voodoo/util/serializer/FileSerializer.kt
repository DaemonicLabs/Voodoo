package voodoo.util.serializer

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import java.io.File

@Serializer(forClass = File::class)
object FileSerializer : KSerializer<File> {
    override fun serialize(encoder: Encoder, obj: File) {
        encoder.encodeString(obj.path)
    }

    override fun deserialize(decoder: Decoder): File {
        return File(decoder.decodeString())
    }
}