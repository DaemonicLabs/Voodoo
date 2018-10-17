package voodoo.util.serializer

import kotlinx.serialization.KInput
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializer(forClass = LocalDateTime::class)
object TimestampSerializer : KSerializer<LocalDateTime> {
    override fun save(output: KOutput, obj: LocalDateTime) {
        val epoch = obj.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
        output.writeLongValue(epoch)
    }

    override fun load(input: KInput): LocalDateTime {
        val timestamp = input.readStringValue()
        return timestamp.toLongOrNull()?.let { milliseconds ->
            LocalDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZoneId.of("UTC"))
        } ?: LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}
