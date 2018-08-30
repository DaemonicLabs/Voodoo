package voodoo.data.curse

import blue.endless.jankson.JsonPrimitive
import blue.endless.jankson.impl.Marshaller

inline class ProjectID(val value: Int) {
    override fun toString(): String {
        return value.toString()
    }

    val valid: Boolean
        get() = value > 0

    companion object {
        val INVALID = ProjectID(-1)

        fun fromJson(jsonObj: Any) =
                ProjectID(
                        value = (jsonObj as Long).toInt()
                )


        fun toJson(projectId: ProjectID, marshaller: Marshaller): JsonPrimitive {
            return JsonPrimitive(projectId.value)
        }
    }
}