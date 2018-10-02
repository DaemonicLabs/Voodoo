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
import com.squareup.kotlinpoet.asTypeName
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import mu.KLogging
import voodoo.curse.CurseClient
import voodoo.data.ForgeVersion
import voodoo.data.curse.ProjectID
import voodoo.dsl.CurseMod
import voodoo.forge.Forge
import java.io.File

fun main(vararg args: String) {
    poet(root = File(args[0]))
}

fun poet(
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

    runBlocking {
        Poet.generate(
            name = mods,
            slugIdMap = Poet.requestMods(),
            slugSanitizer = slugSanitizer,
            folder = root
        )

        Poet.generate(
            name = texturePacks,
            slugIdMap = Poet.requestResourcePacks(),
            slugSanitizer = slugSanitizer,
            folder = root
        )

        Poet.generateForge("Forge", folder = root)
    }
}

object Poet : KLogging() {
    internal fun generate(
        name: String,
        slugIdMap: Map<String, ProjectID>,
        slugSanitizer: (String) -> String,
        folder: File
    ) {
        val curseModType = CurseMod::class.asTypeName()
        val objectBuilder = TypeSpec.objectBuilder(name)
        slugIdMap.entries.sortedBy { (slug, id) ->
            slug
        }.forEach { (slug, id) ->
            objectBuilder.addProperty(
                PropertySpec.builder(
                    slugSanitizer(slug),
                    Int::class,
                    KModifier.CONST
                )
                    .mutable(false)
                    .initializer("%L", id.value)
                    .build()
            )
        }


        save(objectBuilder.build(), name, folder)
    }

    internal suspend fun generateForge(
        name: String,
        folder: File
    ) {
        val forgeData = runBlocking {
            Forge.getForgeData()
        }

        val forgeVersionType = ForgeVersion::class.asTypeName()

        fun ForgeVersion.toPropertySpec(identifier: String): PropertySpec {
            return PropertySpec.builder(identifier, forgeVersionType)
                .initializer("%T(%S, %S, %S, %S, %L)", forgeVersionType, url, fileName, longVersion, forgeVersion, build).build()
        }

        val forgeBuilder = TypeSpec.objectBuilder("Forge")
        val webpath =
            PropertySpec.builder("WEBPATH", String::class, KModifier.CONST).initializer("%S", forgeData.webpath).build()
        forgeBuilder.addProperty(webpath)

        for ((version, numbers) in forgeData.mcversion) {
            val versionIdentifier = "mc" + version.replace('.', '_')
            val versionBuilder = TypeSpec.objectBuilder(versionIdentifier)
            for (number in numbers) {
                val buildIdentifier = "build$number"
                val forgeVersion = Forge.toForgeVersion(number)
                val property = forgeVersion.toPropertySpec(buildIdentifier)
                versionBuilder.addProperty(property)
            }
            forgeBuilder.addType(versionBuilder.build())
        }

        for ((key, number) in forgeData.promos) {
            val keyIdentifier = key.replace('-', '_').replace('.', '_').run {
                if (this.first().isDigit())
                    "mc$this"
                else
                    this
            }
            val forgeVersion = Forge.toForgeVersion(number)
            val property = forgeVersion.toPropertySpec(keyIdentifier)
            forgeBuilder.addProperty(property)
        }

        save(forgeBuilder.build(), name, folder)
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
    private data class SlugIdPair(
        val id: Int,
        val slug: String
    )

    @Serializable
    private data class WrapperAddonResult(
        val addons: List<SlugIdPair>
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