package newformat.builder

import newformat.modpack.Condition
import newformat.modpack.Feature
import newformat.modpack.entry.FileInstall

class PropertiesApplicator(
    var userFiles: FnPatternList
) {
    private val used = HashSet<Feature>()
    private val features = ArrayList<FeaturePattern>()


    val featuresInUse: List<Feature>
        get() = ArrayList(used)

    fun apply(entry: FileInstall) {
        val path = entry.targetPath
        entry.conditionWhen = fromFeature(path)
        entry.userFile = isUserFile(path)
    }

    fun isUserFile(path: String): Boolean {
        return userFiles.matches(path)
    }

    fun fromFeature(path: String): Condition? {
        val found = ArrayList<Feature>()
        for (pattern in features) {
            if (pattern.matches(path)) {
                used.add(pattern.feature)
                found.add(pattern.feature)
            }
        }
        return if (!found.isEmpty()) {
            Condition.requireAny(found)
        } else {
            null
        }
    }

    fun register(component: FeaturePattern) {
        features.add(component)
    }
}
