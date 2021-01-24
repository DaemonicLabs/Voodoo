package voodoo.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.UserAgent
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.util.KtorExperimentalAPI
import voodoo.util.download.GeneratedConstants

const val useragent = "voodoo/${GeneratedConstants.FULL_VERSION} (https://github.com/DaemonicLabs/Voodoo)"
// "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36" // ""voodoo/$VERSION (https://github.com/elytra/Voodoo)"

inline fun <T> useClient(createClient: () -> HttpClient = {
    HttpClient(OkHttp) {
        install(UserAgent) {
            agent = useragent
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }
}, block: (HttpClient) -> T) = createClient().use {
    block(it)
}