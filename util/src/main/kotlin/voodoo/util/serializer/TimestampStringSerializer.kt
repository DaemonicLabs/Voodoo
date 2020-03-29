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
object TimestampStringSerializer : KSerializer<LocalDateTime> {
    override fun serialize(encoder: Encoder, obj: LocalDateTime) {
        val timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(obj)
        encoder.encodeString(timestamp)
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val timestamp = decoder.decodeString()
        return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}
