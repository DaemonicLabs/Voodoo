package voodoo.util.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer

@Deprecated("use ListSerializer", ReplaceWith("kotlinx.serialization.builtins.ListSerializer(this)"))
private val <T> KSerializer<T>.list: KSerializer<List<T>> get() = ListSerializer(this)