package newformat.builder

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.EnumSet

@Serializable
data class FnPatternList(

    var include: List<String> = arrayListOf(),

    var exclude: List<String> = arrayListOf(),
    @Transient
    var flags: EnumSet<FnMatch.Flag> = DEFAULT_FLAGS
) {

    fun matches(path: String): Boolean {
        return matches(path, this.include) && !matches(path, this.exclude)
    }

    private fun matches(path: String, patterns: Collection<String>): Boolean {
        for (pattern in patterns) {
            if (FnMatch.match(pattern = pattern, string = path, flags = flags)) {
                return true
            }
        }
        return false
    }

    companion object {
        private val DEFAULT_FLAGS = EnumSet.of<FnMatch.Flag>(FnMatch.Flag.CASEFOLD, FnMatch.Flag.PERIOD)
    }
}
