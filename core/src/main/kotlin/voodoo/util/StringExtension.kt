package voodoo.util

/**
 * Created by nikky on 06/06/18.
 * @author Nikky
 * @version 1.0
 */

val String?.blankOr: String?
    get() = if(this.isNullOrBlank()) null else this