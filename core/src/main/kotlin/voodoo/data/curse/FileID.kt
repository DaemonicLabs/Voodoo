package voodoo.data.curse

import com.fasterxml.jackson.annotation.JsonCreator
import kotlinx.serialization.*
import kotlinx.serialization.internal.PrimitiveDesc

//TODO: inline
@Serializable(with = FileID.Companion::class)
class FileID(val value: Int) {
    override fun toString(): String {
        return value.toString()
    }

    val valid: Boolean
        get() = value > 0

    companion object : KSerializer<FileID>{
        override val serialClassDesc = PrimitiveDesc("FileID")

        override fun load(input: KInput): FileID {
            return FileID(input.readIntValue())
        }

        override fun save(output: KOutput, obj: FileID) {
            output.writeIntValue(obj.value)
        }

        val INVALID = FileID(-1)

        //TODO: remove in 1.3
        @JsonCreator
        @JvmStatic
        fun fromString(id: String?): FileID? {
            return if (id == null)
                null
            else {
                id.toIntOrNull()?.let { FileID(it) }
            }
        }
    }
}