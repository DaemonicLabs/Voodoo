package voodoo.util.redirect


import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.features.RedirectException
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.pipeline.*
import io.ktor.util.*
import voodoo.util.encoded

/**
 * [HttpClient] feature that handles http redirect
 */
class HttpRedirectFixed(
        val maxJumps: Int,
        val applyUrlFunction: (String) -> String = {it}
) {

    class Config {
        var maxJumps: Int = 20
        var applyUrlFunction : (String) -> String = { it }
        fun applyUrl(block: (String) -> String) {
            applyUrlFunction = block
        }
    }

    companion object Feature : HttpClientFeature<Config, HttpRedirectFixed> {
        override val key: AttributeKey<HttpRedirectFixed> = AttributeKey("HttpRedirect")

        private val Redirect = PipelinePhase("RedirectPhase")

        override fun prepare(block: Config.() -> Unit): HttpRedirectFixed =
                with(Config().apply(block)) { HttpRedirectFixed(this.maxJumps, this.applyUrlFunction) }

        override fun install(feature: HttpRedirectFixed, scope: HttpClient) {
            scope.requestPipeline.insertPhaseBefore(HttpRequestPipeline.Send, Redirect)
            scope.requestPipeline.intercept(Redirect) { body ->
                repeat(feature.maxJumps) {
                    val call = scope.sendPipeline.execute(context, body) as HttpClientCall

                    if (!call.response.status.isRedirect()) {
                        finish()
                        proceedWith(call)
                        return@intercept
                    }

                    val location = call.response.headers[HttpHeaders.Location] ?: call.response.headers["location"] ?: return@repeat
                    context.url.takeFrom(feature.applyUrlFunction(location))
                }

                throw RedirectException(context.build(), "Redirect limit ${feature.maxJumps} exceeded")
            }
        }
    }
}

private fun HttpStatusCode.isRedirect(): Boolean = when (value) {
    HttpStatusCode.MovedPermanently.value,
    HttpStatusCode.Found.value,
    HttpStatusCode.TemporaryRedirect.value,
    HttpStatusCode.PermanentRedirect.value -> true
    else -> false
}
