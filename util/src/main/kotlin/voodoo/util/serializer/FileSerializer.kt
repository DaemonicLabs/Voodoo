package voodoo.util.serializer

import kotlinx.serialization.KInput
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import java.io.File

@Serializer(forClass = File::class)
class FileSerializer : KSerializer<File> {
    override fun save(output: KOutput, obj: File) {
        output.writeStringValue(obj.path)
    }

    override fun load(input: KInput): File {
        return File(input.readStringValue())
    }
}