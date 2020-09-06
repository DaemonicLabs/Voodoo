package voodoo.data.nested

interface NestedEntryProvider <E: NestedEntry> {
    fun create(): E
}