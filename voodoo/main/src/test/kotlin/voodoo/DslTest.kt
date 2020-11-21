package voodoo

import Forge
import Mod
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import voodoo.data.Side
import voodoo.data.curse.FileType
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.dsl.builder.ModpackBuilder
import java.io.File

internal class DslTest {
    @Test
    fun `load pack`() {
        val rootFolder = File("run").resolve("test").resolve("dsltest").absoluteFile.apply {
            deleteRecursively()
            mkdirs()
        }
        val builder = ModpackBuilder(NestedPack.create()).apply {
            mcVersion = "1.12.2"
            version = "1.0"
            icon = "icon.png"
            authors = listOf("dude", "and", "friends")
            // TODO: type = {recommended, latest} | buildnumber, make sealed class
            modloader {
                forge(version = Forge.mc1_12_2_recommended)
            }
            mods {
                +NestedEntry.Curse {
                    entry.releaseTypes = setOf(FileType.Release, FileType.Beta)

                    +(Mod.botania)
                    +(Mod.rftools)
                }

                inheritProvider {
                    entry.side = Side.SERVER

                    +NestedEntry.Jenkins {
                        entry.jenkinsUrl = "blub"
                        entry.side = Side.SERVER
                        +"matterlink" {
                            entry.job = "elytra/matterlink/master"
                        }
                    }
                }

            }
        }
        val nestedPack = builder.pack

        val modpack = runBlocking {
            nestedPack.flatten(rootFolder, "test_pack")
        }

        val entries = nestedPack.root.flatten()
    }
}
