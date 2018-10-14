package voodoo.dsl.builder

import voodoo.data.nested.NestedEntry
import voodoo.provider.ProviderBase

open class GroupBuilder<T>(
    provider: T,
    entry: NestedEntry
) : AbstractBuilder<T>(provider, entry) where T : ProviderBase
