package com.skcraft.launcher.model.minecraft

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.Serializer
import java.util.regex.Pattern

@Serializer(forClass = Pattern::class)
object PatternSerializer {
    override fun deserialize(decoder: Decoder): Pattern {
        return Pattern.compile(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, obj: Pattern) {
        encoder.encodeString(obj.pattern())
    }
}
