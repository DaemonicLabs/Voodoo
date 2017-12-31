package moe.nikky.util

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by nikky on 13/10/16.
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
object FormatUtil {
    fun fmt(d: Double): String {
        return if (d == d.toLong().toDouble())
            String.format("%d", d.toLong())
        else
            String.format("%s", d)
    }

    fun fmt(d: Double, sign: Boolean): String {
        return if (d == d.toLong().toDouble())
            String.format(if (sign) "%+d" else "%d", d.toLong())
        else
            String.format("%s", d)
    }
}
