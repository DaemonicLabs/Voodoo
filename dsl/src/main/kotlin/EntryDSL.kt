import voodoo.data.OptionalData
import voodoo.data.curse.ProjectID
import voodoo.data.nested.NestedEntry
import voodoo.dsl.builder.EntryBuilder
import voodoo.dsl.builder.GroupBuilder
import voodoo.dsl.builder.ListBuilder
import voodoo.dsl.builder.OptionalBuilder

/**
 * Create new list of subentries
 */
fun <E: NestedEntry> GroupBuilder<E>.list(
    initList: ListBuilder<E>.() -> Unit
): ListBuilder<E> {
    val listBuilder = ListBuilder( this)
    listBuilder.initList()
    // add all entries from list
    entry.entries += listBuilder.entries.map { it.entry }
    return listBuilder
}

// TODO: add include -like functionality ?


fun <E: NestedEntry> E.optional(block: OptionalBuilder.() -> Unit) {
    val optionalData = this.optionalData?.copy() ?: OptionalData()
    val builder = OptionalBuilder(optionalData)
    builder.block()
    this.optionalData = optionalData
}

fun NestedEntry.Curse.replaceDependencies(vararg replacements: Pair<ProjectID, ProjectID>) {
    val mutableMap =  this.replaceDependencies.toMutableMap()
    replacements.forEach { (original, replacement) ->
        mutableMap[original] = replacement
    }
    this.replaceDependencies = mutableMap.toMap()
}
