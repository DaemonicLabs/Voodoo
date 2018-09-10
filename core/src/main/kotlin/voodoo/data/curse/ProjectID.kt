package voodoo.data.curse

import blue.endless.jankson.JsonPrimitive
import blue.endless.jankson.impl.Marshaller
import com.fasterxml.jackson.annotation.JsonCreator

//TODO: inline
data class ProjectID(val value: Int) {
    override fun toString(): String {
        return value.toString()
    }

    val valid: Boolean
        get() = value > 0

    companion object {
        val INVALID = ProjectID(-1)

        //TODO: remove in 1.3
        @JsonCreator
        @JvmStatic
        fun fromString(id: String?): ProjectID? {
            return if (id == null)
                null
            else {
                id.toIntOrNull()?.let { ProjectID(it) }
            }
        }

        fun fromJson(jsonObj: Any) =
                ProjectID(
                        value = (jsonObj as Long).toInt()
                )


        fun toJson(projectId: ProjectID, marshaller: Marshaller): JsonPrimitive {
            return JsonPrimitive(projectId.value)
        }
    }
}