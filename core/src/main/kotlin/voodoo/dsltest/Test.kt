package voodoo.dsltest

import kotlinx.coroutines.runBlocking
import voodoo.data.Side
import voodoo.data.curse.FileType
import voodoo.data.nested.NestedPack
import voodoo.dsl.*
import voodoo.exceptionHandler
import voodoo.provider.CurseProviderThing
import voodoo.provider.JenkinsProviderThing
import java.io.File

fun main(args: Array<String>) {
    val a = NestedPack(
            id = "some-id",
            version = "1.0",
            //TODO: type = File
            icon = "icon.png",
            authors = listOf("dude", "and", "friends"),
            //TODO: type = {recommended, latest} | buildnumber, make sealed class
            forge = "recommended",
            root = root(CurseProviderThing) {
                optionals = false
                releaseTypes = setOf(FileType.RELEASE, FileType.BETA)

                //TODO: use type URL ?
                metaUrl = "https://curse.nikky.moe"
                entries {
                    id("botania") optionals false

                    id("rf-tools") {
                        optionals = false
                    }

                    entry(JenkinsProviderThing) {
                        side = Side.SERVER
                    }.entries {
                        id("matterlink") job "elytra/matterlink/master"
                        id("elytra/btfu/master")
                    }

//                    include("other.kts")
                }
            }
    )

    val modpack = a.flatten()
    val entries = runBlocking(context = exceptionHandler) {
        a.root.flatten(File(""))
    }
    println(a)
    println(modpack)
    for (entry in entries) {
        println(entry)
    }
}


