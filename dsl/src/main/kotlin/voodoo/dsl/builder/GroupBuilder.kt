package voodoo.dsl.builder

import voodoo.data.nested.NestedEntry
import voodoo.provider.ProviderBase

class GroupBuilder<E: NestedEntry>(
    entry: E
) : AbstractBuilder<E>(entry)
