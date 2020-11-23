//package voodoo.pack
//
//import io.ktor.client.request.get
//import kotlinx.coroutines.runBlocking
//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.JsonConfiguration
//import moe.nikky.voodoo.format.modpack.Manifest
//import mu.KotlinLogging
//import org.spekframework.spek2.Spek
//import org.spekframework.spek2.style.specification.describe
//import voodoo.util.client
//
//private val logger = KotlinLogging.logger {}
//
//object MMCSpek : Spek({
//    describe("load pack") {
//        val manifest by memoized {
//            val packUrl = "https://launcher.towerdevs.xyz/descentfrozenhell.json"
//            println("pack url: $packUrl")
//
//            runBlocking {
//                client.get<Manifest>(packUrl)
//            }
//        }
//        it("load") {
//            println(manifest)
//        }
//        it("pack") {
//            val jsonString = Json(JsonConfiguration(prettyPrint = true, encodeDefaults = false)).encodeToString(Manifest.serializer(), manifest)
//            println(jsonString)
//        }
//    }
//})