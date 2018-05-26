package voodoo.curse.data;

import com.fasterxml.jackson.annotation.JsonCreator

enum class DependencyType {
    REQUIRED,
    OPTIONAL,
    EMBEDDED;

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromString(key: String?): DependencyType? {
        return if (key == null)
            null
        else {
            val index = key.toIntOrNull() ?: return valueOf(key.toUpperCase())
            return values()[index - 1]
        }
        }
    }
}