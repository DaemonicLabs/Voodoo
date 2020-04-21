package moe.nikky.voodoo.format.builder

import moe.nikky.voodoo.format.Feature
import moe.nikky.voodoo.format.FeatureWithPattern
import moe.nikky.voodoo.format.FnPatternList
import moe.nikky.voodoo.format.modpack.Condition
import moe.nikky.voodoo.format.modpack.entry.FileInstall

class PropertiesApplicator(
    var userFiles: FnPatternList
) {
    private val used = HashSet<Feature>()
    private val features = ArrayList<FeatureWithPattern>()


    val featuresInUse: List<Feature>
        get() = ArrayList(used)

    fun apply(entry: FileInstall) {
        val path = entry.targetPath
        entry.condition = fromFeature(path)
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

    fun register(component: FeatureWithPattern) {
        features.add(component)
    }
}
