package voodoo.dsl.builder

import com.skcraft.launcher.model.modpack.Feature
import voodoo.property

class FeatureBuilder(feature: Feature) {
    var name by property(feature::name)
    var selected by property(feature::selected)
    var description by property(feature::description)
    var recommendation by property(feature::recommendation)
}