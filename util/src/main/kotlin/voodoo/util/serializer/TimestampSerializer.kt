package voodoo.util.serializer

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.json.JsonInput
import kotlinx.serialization.json.JsonOutput
import kotlinx.serialization.json.JsonPrimitive
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
        val jsonInput = decoder as JsonInput
        val value = jsonInput.decodeJson() as JsonPrimitive
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
