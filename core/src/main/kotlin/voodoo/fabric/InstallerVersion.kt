package voodoo.fabric

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.Serializer

inline class InstallerVersion (
    val version: String
) {
    @Serializer(forClass = InstallerVersion::class)
    companion object {
        override fun serialize(encoder: Encoder, value: InstallerVersion) {
            encoder.encodeString(value.version)
        }

        override fun deserialize(decoder: Decoder): InstallerVersion {
            return InstallerVersion(decoder.decodeString())
        }
    }
}