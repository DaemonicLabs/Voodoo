package voodoo.util.serializer

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import java.util.Date

@Serializer(forClass = Date::class)
object DateSerializer : KSerializer<Date> {
    override fun serialize(output: Encoder, obj: Date) {
        output.encodeLong(obj.time)
    }

    override fun deserialize(input: Decoder): Date {
        return Date(input.decodeLong())
    }
}