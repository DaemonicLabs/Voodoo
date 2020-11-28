package voodoo.util

inline fun <K, reified RV> Map<K, *>.filterValueIsInstance(): Map<K, RV> {
    @OptIn(ExperimentalStdlibApi::class)
    return buildMap {
        this@filterValueIsInstance.filterValueIsInstanceTo(this)
    }
}

inline fun <reified RK: K, reified RV: V, K, V, C : MutableMap<in RK, in RV>> Map<K, V>.filterIstInstance(destination: C): C {
    for ((key, value) in this) if (key is RK && value is RV) destination.put(key, value)
    return destination
}

inline fun <reified RV: V, K, V, C : MutableMap<K, in RV>> Map<K, *>.filterValueIsInstanceTo(destination: C): C {
    for ((key, value) in this) if (value is RV) destination.put(key, value)
    return destination
}

inline fun <reified RK: K, K, V, C : MutableMap<in RK, V>> Map<*, V>.filterKeyIsInstance(destination: C): C {
    for ((key, value) in this) if (key is RK) destination.put(key, value)
    return destination
}
