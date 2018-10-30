package voodoo

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

typealias GetProperty<This, R> = This.() -> KMutableProperty0<R>

inline fun <reified This, reified R> readOnly(
    crossinline getRef: GetProperty<This, R>
) =
    object : ReadOnlyProperty<This, R> {
        override fun getValue(thisRef: This, property: KProperty<*>) = thisRef.getRef().get()
    }

inline fun <reified This, reified R> lazyProperty(
    crossinline getRef: GetProperty<This, R>
) =
    object : ReadWriteProperty<This, R> {
        override fun getValue(thisRef: This, property: KProperty<*>) = thisRef.getRef().get()
        override fun setValue(thisRef: This, property: KProperty<*>, value: R) = thisRef.getRef().set(value)
    }

fun <This, R> property(prop: KMutableProperty0<R>) =
    object : ReadWriteProperty<This, R> {
        override fun getValue(thisRef: This, property: KProperty<*>) = prop.get()
        override fun setValue(thisRef: This, property: KProperty<*>, value: R) = prop.set(value)
    }