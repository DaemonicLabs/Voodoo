package moe.nikky.util

/**
 * Created by nikky on 11/11/16.
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
object ParseUtil {
    fun parseDoubleOr(text: String, fallback: Double): Double {
        return if (text.isEmpty()) fallback else java.lang.Double.parseDouble(text)
    }

    fun parseIntOr(text: String, fallback: Int): Int {
        return if (text.isEmpty()) fallback else Integer.parseInt(text)
    }
}
