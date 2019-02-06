package voodoo

import Forge
import Mod
import job
import kotlinx.coroutines.runBlocking
import list
import metaUrl
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import releaseTypes
import voodoo.builder.Builder
import voodoo.data.Side
import voodoo.data.curse.FileType
import voodoo.data.lock.LockPack
import voodoo.provider.CurseProvider
import voodoo.provider.JenkinsProvider
import voodoo.script.MainScriptEnv
import voodoo.util.json
import withProvider
import java.io.File
import kotlin.test.assertEquals

object RoundTripSpek : Spek({
    describe("load pack") {
        val rootFolder by memoized {
            File("run").resolve("test").resolve("roundtrip").absoluteFile.apply {
                deleteRecursively()
                mkdirs()
            }
        }

        val scriptEnv by memoized {
            MainScriptEnv(rootDir = rootFolder, id = "some-id").apply {
                mcVersion = "1.12.2"
                version = "1.0"
                icon = rootFolder.resolve("icon.png").relativeTo(rootFolder)
                authors = listOf("dude", "and", "friends")
                // TODO: type = {recommended, latest} | buildnumber, make sealed class ?
                forge = Forge.recommended
                root(CurseProvider) {
                    releaseTypes = setOf(FileType.RELEASE, FileType.BETA)

                    // TODO: use type URL ?
                    metaUrl = "https://curse.nikky.moe"
                    list {
                        +(Mod.botania)

                        +(Mod.rftools)

                        withProvider(JenkinsProvider) {
                            side = Side.SERVER
                        }.list {
                            +"matterlink" job "elytra/matterlink/master"
                        }

                        group {
                            side = Side.BOTH
                            optional {
                                selected = false
                            }
                        }.list {
                            +(Mod.laggoggles) configure {
                                description =
                                        "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
                            }

                            +(Mod.sampler) configure {
                                description =
                                        "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
                            }

                            +(Mod.openeye) configure {
                                description =
                                        "Automatically collects and submits crash reports. Enable if asked or wish to help sort issues with the pack."
                            }
                        }

//                    include("other.kts")
                    }
                }
            }
        }

        val targetFilename = "roundtrip.lock.json"
        val modpack by memoized {
            scriptEnv.pack.flatten()
        }
        val lockpack by memoized {
            runBlocking {
                Builder.build(modpack, "roundtrip", targetFilename)
            }
        }
        it("parse lockpack") {
            val jsonText = json.stringify(LockPack.serializer(), lockpack)
            println(jsonText)
            val parsedLockPack = json.parse(LockPack.serializer(), jsonText)
            parsedLockPack.rootDir = rootFolder
            println(lockpack)
            println(parsedLockPack)
            assertEquals(lockpack, parsedLockPack)
        }
    }
})
