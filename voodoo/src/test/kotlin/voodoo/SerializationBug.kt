package voodoo

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule
import voodoo.data.lock.LockEntry
import voodoo.util.jsonConfiguration

interface Foo {
    val name: String
}

@Serializable
data class FooData (
    override val name: String = "Foo"
): Foo

@Polymorphic
@Serializable
sealed class Seal {
    @Serializable
    data class Bar(
        val foo: FooData = FooData(),
        val number: Int = 42
    ): Seal(), Foo by foo
    {

    }

    companion object {
        val json = Json(JsonConfiguration.Stable, context = SerializersModule{
            polymorphic<Seal> {
                Seal.Bar::class to Seal.Bar.serializer()
            }
        })
    }
}

val json = Json(jsonConfiguration, context = SerializersModule {
    polymorphic<LockEntry> {
        LockEntry.Curse::class to LockEntry.Curse.serializer()
        LockEntry.Direct::class to LockEntry.Direct.serializer()
        LockEntry.Jenkins::class to LockEntry.Jenkins.serializer()
        LockEntry.Local::class to LockEntry.Local.serializer()
        LockEntry.UpdateJson::class to LockEntry.UpdateJson.serializer()
    }
})

fun main() {
    val barString = Seal.json.stringify(Seal.serializer(), Seal.Bar())

    println(barString)
    LockEntry.serializer()
    LockEntry.Curse()
    val curseJson = json.stringify(LockEntry.serializer(), LockEntry.Curse())
    println(curseJson)
    LockEntry.Jenkins()
    val jenkinsJson = json.stringify(LockEntry.serializer(), LockEntry.Jenkins())
    println(jenkinsJson)
    println(LockEntry.Curse::class.simpleName)
}