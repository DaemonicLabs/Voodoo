package voodoo.data.curse;

import com.fasterxml.jackson.annotation.JsonCreator
import kotlinx.serialization.KInput
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.internal.PrimitiveDesc
import mu.KLogging
import voodoo.data.Side

enum class DependencyType {
    REQUIRED,
    OPTIONAL,
    EMBEDDED;

    companion object : KLogging() {
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