package voodoo.data.curse;

import com.fasterxml.jackson.annotation.JsonCreator
import kotlinx.serialization.KInput
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.internal.PrimitiveDesc

enum class FileType {
    RELEASE,
    BETA,
    ALPHA;

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromString(key: String?): FileType? {
            return if (key == null)
                null
            else {
                val index = key.toIntOrNull() ?: return valueOf(key.toUpperCase())
                return values()[index - 1]
            }
        }

        fun fromJson(element: Any): FileType {
            return valueOf(element.toString().toUpperCase())
        }
    }
}