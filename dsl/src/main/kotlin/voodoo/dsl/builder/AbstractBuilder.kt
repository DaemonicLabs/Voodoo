package voodoo.dsl.builder

import voodoo.data.nested.NestedEntry
import voodoo.dsl.VoodooDSL

@VoodooDSL
abstract class AbstractBuilder<E: NestedEntry>(
    open val entry: E
)