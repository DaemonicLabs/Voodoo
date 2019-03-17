package voodoo.dsl.builder

import voodoo.data.OptionalData
import voodoo.data.curse.ProjectID
import voodoo.data.nested.NestedEntry
import voodoo.dsl.VoodooDSL
import voodoo.property
import voodoo.provider.ProviderBase
import java.io.File

@VoodooDSL
abstract class AbstractBuilder<P : ProviderBase>(
    val provider: P,
    val entry: NestedEntry
) {
    init {
        entry.provider = provider.id
    }

    suspend fun flatten(parent: File) = entry.flatten(parent)

    var folder by property(entry::folder)
//    var comment by property(entry::comment)
    var description by property(entry::description)

    var side by property(entry::side)

    // TODO: depenencies
    //  replaceDependencies

    var packageType by property(entry::packageType)
    //    var transient by property(entry::transient::get, entry::transient::set)
    var version by property(entry::version)
    var fileName by property(entry::fileName)
    var validMcVersions by property(entry::validMcVersions)

    fun optional(block: OptionalBuilder.() -> Unit) {
        val optionalData = entry.optionalData?.copy() ?: OptionalData()
        val builder = OptionalBuilder(optionalData)
        builder.block()
        entry.optionalData = optionalData
    }

    @Deprecated("use optional instead", ReplaceWith("optional(block)"))
    fun feature(block: OptionalBuilder.() -> Unit) {
        val optionalData = entry.optionalData?.copy() ?: OptionalData()
        val builder = OptionalBuilder(optionalData)
        builder.block()
        entry.optionalData = optionalData
    }

    fun replaceDependencies(vararg replacements: Pair<ProjectID, ProjectID>) {
        val mutableMap =  entry.replaceDependencies.toMutableMap()
        replacements.forEach { (original, replacement) ->
            mutableMap[original] = replacement
        }
        entry.replaceDependencies = mutableMap.toMap()
    }
}