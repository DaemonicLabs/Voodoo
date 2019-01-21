package voodoo.pack

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.skcraft.launcher.model.modpack.Manifest
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.curse.CurseClient
import kotlin.test.fail

private val logger = KotlinLogging.logger {}

object MMCSpek : Spek({
    describe("load pack") {
        val modpack by memoized {
            val packUrl = "https://launcher.towerdevs.xyz/descentfrozenhell.json"
            println("pack url: $packUrl")

            val (request, response, result) = packUrl.httpGet()
                .header("User-Agent" to CurseClient.useragent)
                .responseString()
            when (result) {
                is Result.Success -> Json(strictMode = false).parse(Manifest.serializer(), result.value)
                is Result.Failure -> {
                    logger.error(result.error.exception) { "could not retrieve pack, ${result.error}" }
                    fail("http request failed")
                }
            }
        }
        it("load") {
            println(modpack)
        }
        it("pack") {
            val jsonString = Json(indented = true, encodeDefaults = false).stringify(Manifest.serializer(), modpack)
            println(jsonString)
        }
    }
})