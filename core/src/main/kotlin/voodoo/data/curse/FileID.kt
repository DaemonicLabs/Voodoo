package voodoo.data.curse

import blue.endless.jankson.JsonPrimitive
import blue.endless.jankson.impl.Marshaller
import com.fasterxml.jackson.annotation.JsonCreator

//TODO: inline
class FileID(val value: Int) {
    override fun toString(): String {
        return value.toString()
    }

    val valid: Boolean
        get() = value > 0

    companion object {
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

        fun fromJson(jsonObj: Any) =
                FileID(
                        value = (jsonObj as Long).toInt()
                )


        fun toJson(fileId: FileID, marshaller: Marshaller): JsonPrimitive {
            return JsonPrimitive(fileId.value)
        }
    }
}