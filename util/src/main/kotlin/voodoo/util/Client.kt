package voodoo.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import voodoo.util.redirect.HttpRedirectFixed
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import kotlinx.coroutines.experimental.runBlocking
import java.lang.Exception


private val client = HttpClient(Apache) {
    engine {
//        maxConnectionsCount = 1000 // Maximum number of socket connections.
//        endpoint.apply {
//            maxConnectionsPerRoute = 100 // Maximum number of requests for a specific endpoint route.
//            pipelineMaxSize = 20 // Max number of opened endpoints.
//            keepAliveTime = 5000 // Max number of milliseconds to keep each connection alive.
//            connectTimeout = 5000 // Number of milliseconds to wait trying to connect to the server.
//            connectRetryAttempts = 5 // Maximum number of attempts for retrying a connection.
//        }
//        config {
//            followRedirects(true)
//        }
    }
//    defaultRequest {
//        header("User-Agent", "")
//    }
    install(HttpRedirectFixed) {
        applyUrl { it.encoded }
    }
    install(JsonFeature) {
        serializer = KotlinxSerializer()
//        serializer = JacksonSerializer {
//            //                registerModule(KotlinModule()) // Enable Kotlin support
//            configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)!!
//        }
    }
}

data class TestData(
        val a: Int
)

fun main(args: Array<String>) = runBlocking {
    val test = try {
        client.get<TestData>("https://goofsgyfdygle.com")
    } catch(e: Exception) {
        e.printStackTrace()
        null
    }
    println(test)


}