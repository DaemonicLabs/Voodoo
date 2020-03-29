//package voodoo.util
//
//import io.ktor.client.HttpClient
//import io.ktor.client.engine.cio.CIO
//import io.ktor.client.features.UserAgent
//import io.ktor.client.features.json.JsonFeature
//import io.ktor.client.features.json.serializer.KotlinxSerializer
//import io.ktor.util.KtorExperimentalAPI
//import voodoo.util.UtilConstants.VERSION
//import io.ktor.client.request.*
//import io.ktor.client.statement.*
//import io.ktor.http.*
//import io.ktor.util.cio.*
//import io.ktor.utils.io.*
//import java.io.*
//import java.net.*
//
//const val useragent = "voodoo/$VERSION (https://github.com/DaemonicLabs/Voodoo)"
//
//@OptIn(KtorExperimentalAPI::class)
//val client = HttpClient(CIO) {
//    install(UserAgent) {
//        agent = useragent
//    }
//    install(JsonFeature) {
//        serializer = KotlinxSerializer()
//    }
//}
//
//
//data class HttpClientException(val response: HttpResponse) : IOException("HTTP Error ${response.status}")
//
//suspend fun HttpClient.getAsTempFile(url: String, callback: suspend (file: File) -> Unit) {
//    val file = getAsTempFile(url)
//    try {
//        callback(file)
//    } finally {
//        file.delete()
//    }
//}
//
//suspend fun HttpClient.getAsTempFile(url: String): File {
//    val file = File.createTempFile("ktor", "http-client")
//    val response = request<HttpResponse> {
//        url(URL(url))
//        method = HttpMethod.Get
//    }
//    if (!response.status.isSuccess()) {
//        throw HttpClientException(response)
//    }
//    response.content.copyAndClose(file.writeChannel())
//    return file
//}