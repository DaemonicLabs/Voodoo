package voodoo

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

inline fun <reified This, reified R> lazyReadOnly(
    crossinline getRef: This.() -> KProperty0<R>
) =
    object : ReadOnlyProperty<This, R> {
        override fun getValue(thisRef: This, property: KProperty<*>) = thisRef.getRef().get()
    }

inline fun <reified This, reified R> lazyProperty(
    crossinline getRef: This.() -> KMutableProperty0<R>
) =
    object : ReadWriteProperty<This, R> {
        override fun getValue(thisRef: This, property: KProperty<*>) = thisRef.getRef().get()
        override fun setValue(thisRef: This, property: KProperty<*>, value: R) = thisRef.getRef().set(value)
    }

fun <This, R> readOnly(prop: KProperty0<R>) =
    object : ReadOnlyProperty<This, R> {
        override fun getValue(thisRef: This, property: KProperty<*>) = prop.get()
    }

fun <This, R> property(prop: KMutableProperty0<R>) =
    object : ReadWriteProperty<This, R> {
        override fun getValue(thisRef: This, property: KProperty<*>) = prop.get()
        override fun setValue(thisRef: This, property: KProperty<*>, value: R) = prop.set(value)
    }