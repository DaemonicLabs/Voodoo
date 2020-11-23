package voodoo.util

object VersionComparator: Comparator<String> {
    override fun compare(semVer1: String, semVer2: String): Int {
        val versionComponents1 = semVer1.split(".").map { it.toInt() }
        val versionComponents2 = semVer2.split(".").map { it.toInt() }
        val maxIndex = kotlin.math.max(versionComponents1.lastIndex, versionComponents2.lastIndex)

        (0..maxIndex).forEach { index ->
            val versionNumber1 = versionComponents1.getOrElse(index) { 0 }
            val versionNumber2 = versionComponents2.getOrElse(index) { 0 }
            if (versionNumber1 != versionNumber2)
                return versionNumber1 - versionNumber2
        }
        return 0
    }
}