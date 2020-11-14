package voodoo.util

fun <K, V, T: V> Map<K,V>.filterValueIsInstance(): Map<K,T> {
    return this.mapNotNull {(key, value) ->
        (value as? T)?.let {
            key to it
        }
    }.toMap()
}