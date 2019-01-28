package voodoo.dsl.builder

import com.skcraft.launcher.model.modpack.Feature
import voodoo.data.nested.NestedEntry
import voodoo.dsl.VoodooDSL
import voodoo.property
import voodoo.provider.ProviderBase
import java.io.File

@VoodooDSL
abstract class AbstractBuilder<P : ProviderBase>(
    open val provider: P,
    open val entry: NestedEntry
) {
    init {
        entry.provider = provider.id
    }

    suspend fun flatten(parent: File) = entry.flatten(parent)

    var folder by property(entry::folder)
    var comment by property(entry::comment)
    var description by property(entry::description)

    var side by property(entry::side)

    // TODO: depenencies
    //  replaceDependencies

    var packageType by property(entry::packageType)
    //    var transient by property(entry::transient::get, entry::transient::set)
    var version by property(entry::version)
    var fileName by property(entry::fileName)
    var validMcVersions by property(entry::validMcVersions)

    fun feature(block: FeatureBuilder.() -> Unit) {
        val feature = entry.feature?.copy() ?: Feature()
        val wrapper = FeatureBuilder(feature)
        wrapper.block()
        entry.feature = feature
    }
}