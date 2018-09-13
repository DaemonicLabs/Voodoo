package voodoo.data

import kotlinx.serialization.KInput
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerialClassDesc
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.PrimitiveDesc

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */
enum class Side(@Transient val flag: Int) {
    CLIENT(1), SERVER(2), BOTH(3);
    infix operator fun plus(other: Side) = Side.values().find { it.flag == this.flag or other.flag }!!

    companion object : KSerializer<Side> {
        override val serialClassDesc = PrimitiveDesc("Side")
        override fun load(input: KInput) = Side.valueOf(input.readStringValue())
        override fun save(output: KOutput, obj: Side) {
            output.writeStringValue(obj.name)
        }
    }
}