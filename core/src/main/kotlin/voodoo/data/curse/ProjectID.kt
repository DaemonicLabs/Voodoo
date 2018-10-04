package voodoo.data.curse

import kotlinx.serialization.KInput
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.PrimitiveDesc

// TODO: inline
@Serializable(with = ProjectID.Companion::class)
data class ProjectID(val value: Int) {
    override fun toString(): String {
        return value.toString()
    }

    val valid: Boolean
        get() = value > 0

    companion object : KSerializer<ProjectID> {
        override val serialClassDesc = PrimitiveDesc("ProjectID")

        override fun load(input: KInput): ProjectID {
            return ProjectID(input.readIntValue())
        }

        override fun save(output: KOutput, obj: ProjectID) {
            output.writeIntValue(obj.value)
        }

        val INVALID = ProjectID(-1)
    }
}