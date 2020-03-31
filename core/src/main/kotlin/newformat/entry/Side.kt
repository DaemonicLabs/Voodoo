package newformat.entry;

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */
enum class Side(@Transient val flag: Int) {
    CLIENT(0b01), SERVER(0b10), BOTH(0b11);

    infix operator fun plus(other: Side) = Side.values().find { it.flag == this.flag or other.flag }!!
}