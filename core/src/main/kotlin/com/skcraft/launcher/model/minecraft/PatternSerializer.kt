package com.skcraft.launcher.model.minecraft

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import java.util.regex.Pattern

@Serializer(forClass = Pattern::class)
object PatternSerializer {
    override fun deserialize(input: Decoder): Pattern {
        return Pattern.compile(input.decodeString())
    }

    override fun serialize(output: Encoder, obj: Pattern) {
        output.encodeString(obj.pattern())
    }
}
