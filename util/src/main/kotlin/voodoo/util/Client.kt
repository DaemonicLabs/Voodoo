package voodoo.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.UserAgent
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.util.KtorExperimentalAPI
import voodoo.util.UtilConstants.VERSION

const val useragent = "voodoo/$VERSION (https://github.com/DaemonicLabs/Voodoo)"

@OptIn(KtorExperimentalAPI::class)
val client = HttpClient(CIO) {
    install(UserAgent) {
        agent = useragent
    }
    install(JsonFeature) {
        serializer = KotlinxSerializer()
    }
}