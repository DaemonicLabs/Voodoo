package voodoo.data.curse

import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import voodoo.data.DependencyType

enum class CurseDependencyType(val depType: DependencyType? = null) {
    // Token: 0x04000055 RID: 85
    EmbeddedLibrary,
    // Token: 0x04000056 RID: 86
    OptionalDependency(DependencyType.OPTIONAL),
    // Token: 0x04000057 RID: 87
    RequiredDependency(DependencyType.REQUIRED),
    // Token: 0x04000058 RID: 88
    Tool,
    // Token: 0x04000059 RID: 89
    Incompatible,
    // Token: 0x0400005A RID: 90
    Include;


    @Serializer(forClass = CurseDependencyType::class)
    companion object {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CurseDependencyType", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): CurseDependencyType {
            return CurseDependencyType.values()[decoder.decodeInt()-1]
        }

        override fun serialize(encoder: Encoder, value: CurseDependencyType) {
            encoder.encodeInt(value.ordinal + 1)
        }
    }
}