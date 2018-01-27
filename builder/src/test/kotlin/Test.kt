import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import moe.nikky.builder.*
import voodoo.builder.provider.CurseProviderThing
import voodoo.builder.provider.DependencyType
import voodoo.builder.provider.DirectProviderThing
import org.junit.Test
import voodoo.builder.Entry
import voodoo.builder.Modpack
import voodoo.builder.Provider

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class EntryTest {
    /**
     * Created by nikky on 30/12/17.
     * @author Nikky
     * @version 1.0
     */
    @Test
    fun testEntry() {
        print("test")
    }


    @Test
    fun conversionTest() {
        val entry = Entry(name = "test", comment = "lets convert some stuff", provider = Provider.CURSE)
        var thingy = entry.provider.thingy
        println(thingy.name)

        entry.provider = Provider.DIRECT
        thingy = entry.provider.thingy
        println(thingy.name)
//        var data = entry.providerData
//        (entry.providerData as ProviderData.CurseEntry).id = 12345
//        println(entry)
//        var entry2 = entry.convert<ProviderData.JenkinsEntry>()
//        when(entry2.providerData) {
//            is ProviderData.JenkinsEntry -> (entry2.providerData as ProviderData.JenkinsEntry).job = "someJob"
//        }
//        println(entry2)
//        var entry3 = entry2.convert<ProviderData.LocalEntry>()
//        println(entry3)
//        var entry4 = entry3.convert<ProviderData.CurseEntry>()
//        println(entry4)
    }

    @Test
    fun seralizeTest() {
        var pack = Modpack("pack name")
        pack.mods.entries += Entry(
                name = "test"
        )
        pack.mods.entries += Entry(
                id = 2,
                provider = Provider.DIRECT
        )
        pack.mods.entries += Entry(
                path = "somwhere",
                provider = Provider.DIRECT,
                dependencies = mutableMapOf(Pair(DependencyType.optional, listOf(pack.mods.entries.first().name))
                )
        )


        val path = System.getProperty("user.dir")

        println("Working Directory = $path")
        writeToFile(Paths.get("$path/test.yaml"), pack)
        val pack2 = loadFromFile(Paths.get("$path/test.yaml"))
        println(pack2)
        for (entry: Entry in pack2.mods.entries) {
            val thingy = entry.provider.thingy
            println(thingy.name)
            when(thingy) {
                is CurseProviderThing -> {

                }
                is DirectProviderThing -> {
                    thingy.doDirectThingy()
                }
            }
//            when(entry.provider) {
//                Provider.CURSE -> {
//                    println("this is a Curse Provider")
//                }
//                Provider.DIRECT -> {
//                    println("this is a Direct Provider")
//                }
//            }
        }
    }

    fun loadFromFile(path: Path): Modpack {
        val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
        mapper.registerModule(KotlinModule()) // Enable Kotlin support

        return Files.newBufferedReader(path).use {
            mapper.readValue(it, Modpack::class.java)
        }
    }
    fun writeToFile(path: Path, pack: Modpack) {
        val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
        mapper.registerModule(KotlinModule()) // Enable Kotlin support

        return Files.newBufferedWriter(path).use {
            mapper.writeValue(it, pack)
        }
    }
}