package voodoo.dsl.builder

import voodoo.data.nested.NestedEntry
import voodoo.dsl.VoodooDSL

@VoodooDSL
class GroupBuilder<E: NestedEntry>(
    entry: E
) : AbstractBuilder<E>(entry)
