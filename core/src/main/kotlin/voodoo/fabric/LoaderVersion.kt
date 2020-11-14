package voodoo.fabric

import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

inline class LoaderVersion (
    val version: String
) {
    @Serializer(forClass = LoaderVersion::class)
    companion object {
        override fun serialize(encoder: Encoder, value: LoaderVersion) {
            encoder.encodeString(value.version)
        }

        override fun deserialize(decoder: Decoder): LoaderVersion {
            return LoaderVersion(decoder.decodeString())
        }
    }
}