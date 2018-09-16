package voodoo.util.serializer

import kotlinx.serialization.KInput
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerialClassDesc
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.SerialClassDescImpl
import java.io.File

@Serializer(forClass = File::class)
class FileSerializer: KSerializer<File> {
    override val serialClassDesc: KSerialClassDesc = SerialClassDescImpl("java.io.File")

    override fun save(output: KOutput, obj: File) {
        output.writeStringValue(obj.path)
    }

    override fun load(input: KInput): File {
        return File(input.readStringValue())
    }
}