package voodoo.mmc.data

import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import voodoo.data.lock.LockPack

@Serializable(with=PackComponent.Companion::class)
data class PackComponent(
    @Optional var uid: String = "",
    @Optional var version: String = "",
    @Optional var cachedName: String = "",
//    @Optional var cachedRequires: Any? = null,
    @Optional var cachedVersion: String = "",
    @Optional var important: Boolean = false,
    @Optional var cachedVolatile: Boolean = false,
    @Optional var dependencyOnly: Boolean = false
) {
    @Serializer(forClass = PackComponent::class)
    companion object : KSerializer<PackComponent> {
        private val DEFAULT = PackComponent()
        override fun save(output: KOutput, obj: PackComponent) {
            val elemOutput = output.writeBegin(serialClassDesc)
            elemOutput.serialize(DEFAULT.uid, obj.uid, 0)
            elemOutput.serialize(DEFAULT.version, obj.version, 1)
            elemOutput.serialize(DEFAULT.cachedName, obj.cachedName, 2)
//            elemOutput.serialize(DEFAULT.cachedRequires, obj.cachedRequires, 0)
            elemOutput.serialize(DEFAULT.cachedVersion, obj.cachedVersion, 3)
            elemOutput.serialize(DEFAULT.important, obj.important, 4)
            elemOutput.serialize(DEFAULT.cachedVolatile, obj.cachedVolatile, 5)
            elemOutput.serialize(DEFAULT.dependencyOnly, obj.dependencyOnly, 6)
            elemOutput.writeEnd(LockPack.serialClassDesc)
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