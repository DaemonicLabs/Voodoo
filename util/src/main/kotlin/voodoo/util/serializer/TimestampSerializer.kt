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
    override fun serialize(output: Encoder, obj: LocalDateTime) {
        val epoch = obj.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
        output.encodeLong(epoch)
    }

    override fun deserialize(input: Decoder): LocalDateTime {
        val timestamp = input.decodeString()
        return timestamp.toLongOrNull()?.let { milliseconds ->
            LocalDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZoneId.of("UTC"))
        } ?: LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}
