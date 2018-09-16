package voodoo.mmc.data

import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

@Serializable(with=CachedRequire.Companion::class)
data class CachedRequire(
    @Optional var uid: String = "",
    @Optional var suggests: String = "",
    @Optional var equals: String = ""
) {
    @Serializer(forClass = CachedRequire::class)
    companion object : KSerializer<CachedRequire> {
        private val DEFAULT = CachedRequire()
        override fun save(output: KOutput, obj: CachedRequire) {
            val elemOutput = output.writeBegin(serialClassDesc)
            elemOutput.serialize(DEFAULT.uid, obj.uid, 0)
            elemOutput.serialize(DEFAULT.suggests, obj.suggests, 1)
            elemOutput.serialize(DEFAULT.equals, obj.equals, 2)
            elemOutput.writeEnd(serialClassDesc)
        }

        private inline fun <reified T : Any> KOutput.serialize(default: T, actual: T, index: Int) {
            if (default != actual) {
                when (actual) {
                    is String -> this.writeStringElementValue(serialClassDesc, index, actual)
                    is Int -> this.writeIntElementValue(serialClassDesc, index, actual)
                    is Boolean -> this.writeBooleanElementValue(serialClassDesc, index, actual)
                }
            }
        }
    }
}