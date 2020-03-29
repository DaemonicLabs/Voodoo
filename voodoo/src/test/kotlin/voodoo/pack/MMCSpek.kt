package voodoo.pack

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.skcraft.launcher.model.modpack.Manifest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KotlinLogging
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.curse.CurseClient
import kotlin.test.fail

private val logger = KotlinLogging.logger {}

object MMCSpek : Spek({
    describe("load pack") {
        val manifest by memoized {
            val packUrl = "https://launcher.towerdevs.xyz/descentfrozenhell.json"
            println("pack url: $packUrl")

            val (request, response, result) = packUrl.httpGet()
                .header("User-Agent" to CurseClient.useragent)
                .responseString()
            when (result) {
                is Result.Success -> Json(JsonConfiguration(ignoreUnknownKeys = true)).parse(Manifest.serializer(), result.value)
                is Result.Failure -> {
                    logger.error(result.error.exception) { "could not retrieve pack, ${result.error}" }
                    fail("http request failed")
                }
            }
        }
        it("load") {
            println(manifest)
        }
        it("pack") {
            val jsonString = Json(JsonConfiguration(prettyPrint = true, encodeDefaults = false)).stringify(Manifest.serializer(), manifest)
            println(jsonString)
        }
    }
})