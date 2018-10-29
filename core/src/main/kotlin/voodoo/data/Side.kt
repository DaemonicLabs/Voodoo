package voodoo.data

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */
enum class Side(@Transient val flag: Int) {
    CLIENT(1), SERVER(2), BOTH(3);

    infix operator fun plus(other: Side) = Side.values().find { it.flag == this.flag or other.flag }!!

//    companion object : KSerializer<Side> {
// //        override val descriptor = PrimitiveDesc("Side")
//        override fun deserialize(input: Decoder) = Side.valueOf(input.readStringValue())
//        override fun serialize(output: Encoder, obj: Side) {
//            output.writeStringValue(obj.name)
//        }
//    }
}