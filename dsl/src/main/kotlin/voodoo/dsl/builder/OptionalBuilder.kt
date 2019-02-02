package voodoo.dsl.builder

import voodoo.data.OptionalData
import voodoo.property

class OptionalBuilder(optionalData: OptionalData) {
//    var displayName by property(optionalData::displayName)
    var selected by property(optionalData::selected)
//    var description by property(optionalData::description)
    var skRecommendation by property(optionalData::skRecommendation)
}