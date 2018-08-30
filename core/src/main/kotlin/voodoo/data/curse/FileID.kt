package voodoo.data.curse

import blue.endless.jankson.JsonPrimitive
import blue.endless.jankson.impl.Marshaller

inline class FileID(val value: Int) {
    override fun toString(): String {
        return value.toString()
    }

    val valid: Boolean
        get() = value > 0

    companion object {
        val INVALID = FileID(-1)

        fun fromJson(jsonObj: Any) =
                FileID(
                        value = (jsonObj as Long).toInt()
                )


        fun toJson(fileId: FileID, marshaller: Marshaller): JsonPrimitive {
            return JsonPrimitive(fileId.value)
        }
    }
}