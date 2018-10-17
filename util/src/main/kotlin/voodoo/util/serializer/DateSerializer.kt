package voodoo.util.serializer

import kotlinx.serialization.KInput
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import java.util.Date

@Serializer(forClass = Date::class)
object DateSerializer : KSerializer<Date> {
    override fun save(output: KOutput, obj: Date) {
        output.writeLongValue(obj.time)
    }

    override fun load(input: KInput): Date {
        return Date(input.readLongValue())
    }
}