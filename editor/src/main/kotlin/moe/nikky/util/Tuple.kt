package moe.nikky.util

/**
 * Created by nikky on 13/10/16.
 * I would like to use apache commons
 * @see [Apache Commons pair](http://commons.apache.org/proper/commons-lang/javadocs/api-3.1/org/apache/commons/lang3/tuple/package-summary.html)
 *
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
class Tuple<X, Y>(val left: X?, val right: Y?) {

    override fun toString(): String {
        return "($left,$right)"
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is Tuple<*, *>) {
            return false
        }
        val other_ = other as Tuple<X, Y>?


        // this may cause NPE if nulls are valid values for x or y. The logic may be improved to handle nulls properly, if needed.
        return other_!!.left == this.left && other_.right == this.right
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + (left?.hashCode() ?: 0)
        result = prime * result + (right?.hashCode() ?: 0)
        return result
    }
}