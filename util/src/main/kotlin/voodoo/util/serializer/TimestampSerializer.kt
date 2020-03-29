package voodoo.util.serializer

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializer(forClass = LocalDateTime::class)
object TimestampSerializer : KSerializer<LocalDateTime> {
    override fun serialize(encoder: Encoder, obj: LocalDateTime) {
        val epoch = obj.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
        encoder.encodeLong(epoch)
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val timestamp = decoder.decodeLong()
        return timestamp.let { milliseconds ->
            LocalDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZoneId.of("UTC"))
        }
    }
}
