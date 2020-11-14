package voodoo.util.serializer

import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializer(forClass = LocalDateTime::class)
object TimestampSerializer : KSerializer<LocalDateTime> {
    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        // TODO: should it always encode as Long ?
        val epoch = value.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
        encoder.encodeLong(epoch)
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val jsonDecoder = decoder as JsonDecoder
        val value = jsonDecoder.decodeJsonElement() as JsonPrimitive
        val timestamp = value.longOrNull
        if(timestamp != null) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("UTC"))
        }
        val timeString = value.contentOrNull
        if(timeString != null) {
            return LocalDateTime.parse(timeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }
        error("value: $value cannot be decoded")
    }
}
