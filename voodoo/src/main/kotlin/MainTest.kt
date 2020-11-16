import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.SequenceStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.github.ricky12awesome.jss.dsl.ExperimentalJsonSchemaDSL
import mu.KotlinLogging
import voodoo.curse.CurseClient
import voodoo.data.Side
import voodoo.data.curse.FileType
import voodoo.data.curse.ProjectID
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.dsl.builder.ModpackBuilder
import voodoo.fabric.IntermediaryVersion
import voodoo.forge.ForgeUtil
import voodoo.util.SharedFolders
import java.io.File

import voodoo.util.json

object MainTest {
    object FabricMod {
        val fabricApi: ProjectID
            inline get() = ProjectID(306612)
        val betternether: ProjectID
            inline get() = ProjectID(311377)
        val tabInventoryFabric: ProjectID
            inline get() = ProjectID(362943)
        val roughlyEnoughItems: ProjectID
            inline get() = ProjectID(310111)
        val roughlyEnoughResources: ProjectID
            inline get() = ProjectID(325625)
        val appleskin: ProjectID
            inline get() = ProjectID(248787)
        val modmenu: ProjectID
            inline get() = ProjectID(308702)
        val mouseWheelie: ProjectID
            inline get() = ProjectID(317514)
    }
    object Fabric {
        object intermediary {
            public val v_1_15_2: IntermediaryVersion
                inline get() = IntermediaryVersion("1.15.2")
        }
    }
    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalJsonSchemaDSL::class)
    @JvmStatic
    fun main(args: Array<String>) {
        SharedFolders.RootDir.value = File("voodoo/schema").absoluteFile

        val builder = ModpackBuilder(NestedPack.create()).apply {
            mcVersion = "1.15.2"
            version = "0.0.1"
            icon = "icon.png"
            authors = listOf("NikkyAi")

            modloader {
                fabric(
                    intermediary = Fabric.intermediary.v_1_15_2
                )
            }

            pack {
                multimc {
                    selfupdateUrl = "https://nikky.moe/.mc/experimental/fabricpack.json"
                }
            }

            mods {
                +NestedEntry.Curse {
                    entry.releaseTypes = setOf(FileType.Release, FileType.Beta)
                    +FabricMod.fabricApi

                    +FabricMod.betternether

                    +FabricMod.tabInventoryFabric

//                    +FabricMod.roughlyEnoughItems {
////            version = "abc"
//                    }
//                    +FabricMod.roughlyEnoughResources
                    +inheritProvider {
                        entry.side = Side.CLIENT
                        +FabricMod.roughlyEnoughItems
                        +FabricMod.roughlyEnoughResources
                        +FabricMod.appleskin
                        +FabricMod.modmenu
                    }

                    +FabricMod.mouseWheelie
                }
            }
        }
        logger.info { "pack: ${builder.pack}" }

        val nestedpackJson = json.encodeToString(NestedPack.serializer(), builder.pack)
        logger.info { nestedpackJson }
    }
}

