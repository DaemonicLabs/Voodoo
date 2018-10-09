package voodoo

import Forge
import group
import id
import job
import kotlinx.coroutines.experimental.runBlocking
import list
import metaUrl
import optionals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import releaseTypes
import rootEntry
import voodoo.data.Side
import voodoo.data.curse.FileType
import voodoo.data.lock.LockPack
import voodoo.data.nested.NestedPack
import voodoo.provider.CurseProvider
import voodoo.provider.JenkinsProvider
import voodoo.util.json
import withProvider
import java.io.File

object RoundTripSpek : Spek({
    describe("load pack") {
        val rootFolder by memoized {
            File("run").resolve("test").resolve("roundtrip").absoluteFile.apply {
                deleteRecursively()
                mkdirs()
            }
        }

        val nestedPack by memoized {
            NestedPack(
                rootDir = rootFolder,
                id = "some-id",
                version = "1.0",
                mcVersion = "1.12.2",
                // TODO: type = File
                icon = rootFolder.resolve("icon.png"),
                authors = listOf("dude", "and", "friends"),
                // TODO: type = {recommended, latest} | buildnumber, make sealed class
                forge = Forge.recommended,
                root = rootEntry(CurseProvider) {
                    optionals = false
                    releaseTypes = setOf(FileType.RELEASE, FileType.BETA)

                    // TODO: use type URL ?
                    metaUrl = "https://curse.nikky.moe"
                    list {
                        id("botania") optionals false

                        id("rf-tools") {
                            optionals = false
                        }

                        withProvider(JenkinsProvider) {
                            side = Side.SERVER
                        }.list {
                            id("matterlink") job "elytra/matterlink/master"
                            id("elytra/btfu/master")
                        }

                        group {
                            side = Side.BOTH
                            feature {
                                selected = false
                            }
                        }.list {
                            id(Mod.laggoggles) {
                                description = "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
                            }

                            id(Mod.sampler) {
                                description = "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
                            }

                            id(Mod.openeye) {
                                description = "Automatically collects and submits crash reports. Enable if asked or wish to help sort issues with the pack."
                            }
                        }

//                    include("other.kts")
                    }
                }
            )
        }

        val targetFilename = "roundtrip.lock.json"
        val modpack by memoized {
            nestedPack.flatten()
        }
        beforeEachTest {
            runBlocking {
                Builder.build(modpack, "roundtrip", targetFilename)
            }
        }
        it("parse lockpack") {
            val jsonText = rootFolder.resolve(targetFilename).readText()
                .replace("\n", "").replace("\\s+".toRegex(), " ")
            println(jsonText)
            val lockPack = json.parse<LockPack>(jsonText)
            println(lockPack)
        }
    }
})
