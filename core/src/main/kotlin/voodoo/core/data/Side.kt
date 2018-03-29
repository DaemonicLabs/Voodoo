package voodoo.core.data

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 * @version 1.0
 */
enum class Side(val flag: Int) {
    CLIENT(1), SERVER(2), BOTH(3);
    infix operator fun plus(other: Side) = Side.values().find { it.flag == this.flag and other.flag }!!
}