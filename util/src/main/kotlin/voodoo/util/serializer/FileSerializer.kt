package voodoo.util.serializer

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import java.io.File

@Serializer(forClass = File::class)
class FileSerializer : KSerializer<File> {
    override fun serialize(output: Encoder, obj: File) {
        output.encodeString(obj.path)
    }

    override fun deserialize(input: Decoder): File {
        return File(input.decodeString())
    }
}