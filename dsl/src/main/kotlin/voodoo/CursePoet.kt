package voodoo

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.serialization.Serializable
import mu.KLogging
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
import voodoo.util.encoded
import voodoo.util.json.TestKotlinxSerializer
import voodoo.util.redirect.HttpRedirectFixed
import java.io.File

fun main(vararg args: String) {
    cursePoet(root = File(args[0]))
}

fun cursePoet(
    root: File = File(System.getProperty("user.dir")),
    mods: String = "Mod",
    texturePacks: String = "TexturePack",
    slugSanitizer: (String) -> String = { slug ->
        slug.split('-').joinToString("") { it.capitalize() }.decapitalize()
    }
) {
    class XY
    println("classloader is of type:" + Thread.currentThread().contextClassLoader)
    println("classloader is of type:" + ClassLoader.getSystemClassLoader())
    println("classloader is of type:" + XY::class.java.classLoader)
    Thread.currentThread().contextClassLoader = XY::class.java.classLoader

    CursePoet.generate(
        name = mods,
        slugIdMap = runBlocking {
            CursePoet.requestMods()
                .mapKeys { (slug, id) ->
                    slugSanitizer(slug)
                }
        },
        folder = root
    )

    CursePoet.generate(
        name = texturePacks,
        slugIdMap = runBlocking {
            CursePoet.requestResourcePacks()
                .mapKeys { (slug, id) ->
                    slugSanitizer(slug)
                }
        },
        folder = root
    )
}

object CursePoet : KLogging() {
    private val client = HttpClient(Apache) {
//        engine { }
        defaultRequest {
            header("User-Agent", CurseClient.useragent)
        }
        install(HttpRedirectFixed) {
            applyUrl { it.encoded }
        }
        install(JsonFeature) {
            serializer = TestKotlinxSerializer()
        }
    }

    internal fun generate(
        name: String,
        slugIdMap: Map<String, ProjectID>,
        folder: File = File("run")
    ) {
        val objectBuilder = TypeSpec.objectBuilder(name)
        slugIdMap.entries.sortedBy { (slug, id) ->
            slug
        }.forEach { (slug, id) ->
            objectBuilder.addProperty(
                PropertySpec.builder(
                    slug,
                    Int::class,
                    KModifier.CONST
                )
                    .mutable(false)
                    .initializer("%L", id)
                    .build()
            )
        }

        save(objectBuilder.build(), folder)
    }

    private fun save(type: TypeSpec, folder: File) {
        folder.mkdirs()
        val source = FileSpec.get("", type)
        val path = folder.apply {
            absoluteFile.parentFile.mkdirs()
            createNewFile()
        }.absoluteFile
        source.writeTo(path)
        logger.info("written to $path")
    }

    @Serializable
    private data class GraphQLRequest(
        val query: String,
        val operationName: String,
        val variables: Map<String, Any> = emptyMap()
    )

    @Serializable
    private data class IdNamePair(
        val id: Int,
        val slug: String
    )

    @Serializable
    private data class WrapperAddonResult(
        val addons: List<IdNamePair>
    )

    @Serializable
    private data class GraphQlResult(
        val data: WrapperAddonResult
    )

    internal suspend fun requestMods(): Map<String, ProjectID> = graphQlRequest("gameID: 432, section: MC_ADDONS")

    internal suspend fun requestResourcePacks(): Map<String, ProjectID> =
        graphQlRequest("gameID: 432, section: TEXTURE_PACKS")

    private suspend fun graphQlRequest(filter: String): Map<String, ProjectID> {
        val url = "https://curse.nikky.moe/graphql"
        CurseClient.logger.debug("post $url")
        val request = GraphQLRequest(
            query = """
                    |{
                    |  addons(gameID: 432, section: MC_ADDONS) {
                    |    id
                    |    slug
                    |  }
                    |}
                """.trimMargin(),
            operationName = "GetNameIDPairs"
        )
        return try {
            client.post<GraphQlResult>(url) {
                contentType(ContentType.Application.Json)
                body = request
            }.data.addons.map { (id, slug) ->
                slug to ProjectID(id)
            }.toMap()
        } catch (e: Exception) {
            logger.error("url: $url")
            throw e
        }
    }
}