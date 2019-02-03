package voodoo.dsl.builder

import voodoo.data.OptionalData
import voodoo.property

class OptionalBuilder(optionalData: OptionalData) {
//    var displayName by property(optionalData::displayName)
    var selected by property(optionalData::selected)
    var skRecommendation by property(optionalData::skRecommendation)
    //    var description by property(optionalData::description)
    @Deprecated("renamed to skRecommendation", ReplaceWith("skRecommendation"))
    var recommendation by property(optionalData::skRecommendation)

}