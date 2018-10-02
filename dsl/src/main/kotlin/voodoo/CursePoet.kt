package voodoo

import awaitObjectResponse
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.serialization.jsonBody
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import com.github.kittinunf.result.Result
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import mu.KLogging
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
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
    internal fun generate(
        name: String,
        slugIdMap: Map<String, ProjectID>,
        folder: File
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


        save(objectBuilder.build(), name, folder)
    }

    private fun save(type: TypeSpec, name: String, folder: File) {
        folder.mkdirs()
        val source = FileSpec.get("", type)
        val path = folder.apply {
            absoluteFile.parentFile.mkdirs()
        }.absoluteFile
        val targetFile = path.resolve("$name.kt")
        source.writeTo(path)
        logger.info("written to $targetFile")
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

    internal suspend fun requestMods(): Map<String, ProjectID> =
        graphQlRequest("gameID: 432, section: MC_ADDONS")

    internal suspend fun requestResourcePacks(): Map<String, ProjectID> =
        graphQlRequest("gameID: 432, section: TEXTURE_PACKS")

    private suspend fun graphQlRequest(filter: String): Map<String, ProjectID> {
        val url = "https://curse.nikky.moe/graphql"
        CurseClient.logger.debug("post $url")
        val requestBody = GraphQLRequest(
            query = """
                    |{
                    |  addons($filter) {
                    |    id
                    |    slug
                    |  }
                    |}
                """.trimMargin(),
            operationName = "GetNameIDPairs"
        )
        val (request, response, result) = Fuel.post(url)
            .jsonBody(body = JSON.stringify(requestBody))
            .header("User-Agent" to CurseClient.useragent)
            .awaitObjectResponse<GraphQlResult>(kotlinxDeserializerOf())

        return when (result) {
            is Result.Success -> {
                result.value.data.addons.map { (id, slug) ->
                    slug to ProjectID(id)
                }.toMap()
            }
            is Result.Failure -> {
                logger.error(result.error.exception) { "cold not request slug-id pairs" }
                throw result.error.exception
            }
        }
    }
}