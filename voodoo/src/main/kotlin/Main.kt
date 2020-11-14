import com.github.ricky12awesome.jss.dsl.ExperimentalJsonSchemaDSL
import com.github.ricky12awesome.jss.encodeToSchema
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import mu.KotlinLogging
import voodoo.curse.CurseClient
import voodoo.forge.ForgeUtil
import java.io.File

object Main {
    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalJsonSchemaDSL::class)
    @JvmStatic
    fun main(args: Array<String>) {
        logger.info { "Hello World" }

        val rootDir = File(".").absoluteFile
        val id = "test"

//        val modpackConfig  = Yaml.default.decodeFromString(
//            ModpackPlain.serializer(),
//            File("test.voodoo.yml").readText()
//        )

        val json = Json {
            prettyPrint = true
        }
        // TODO: generate json later
        // debug
        val schemaFile = rootDir.resolve("schema/modpack.schema.json")
        schemaFile.absoluteFile.parentFile.mkdirs()
        schemaFile.writeText(
            json.encodeToSchema(ModpackPlain.serializer())
        )

        val testFile = File("test.voodoo.json").absoluteFile

        val modpackConfig = json.decodeFromString(
            ModpackPlain.serializer(),
            testFile.readText()
        )
        println("modpack: $modpackConfig")

        modpackConfig.mods.mapValues { (entryId, intitalEntry) ->
            return@mapValues intitalEntry.apply.fold(intitalEntry) { acc, tagId ->
                val tag = modpackConfig.tags[tagId] ?: error("$entryId: tag for id $tagId not found")
                return@fold when {
                    acc is PlainEntry.Curse && tag is PlainTag.Curse -> acc.applyTag(tag)
                    acc is PlainEntry.Direct && tag is PlainTag.Direct -> acc.applyTag(tag)
                    acc is PlainEntry.Jenkins && tag is PlainTag.Jenkins -> acc.applyTag(tag)
                    acc is PlainEntry.Local && tag is PlainTag.Local -> acc.applyTag(tag)
                    tag is PlainTag.Common -> acc.applyTag(tag)
                    else -> intitalEntry
                }
            }
        }

        val generatorsCurse = modpackConfig.generators.filterValues { it is Generator.Curse } as Map<String, Generator.Curse>
        val generatorsForge = modpackConfig.generators.filterValues { it is Generator.Forge } as Map<String, Generator.Forge>
        val generatorsFabric = modpackConfig.generators.filterValues { it is Generator.Fabric } as Map<String, Generator.Fabric>

        runBlocking {
            val curseResults = generatorsCurse.mapValues { (generatorId, generator) ->
                CurseClient.graphQLRequest(
                    section = generator.section.sectionName,
                    categories = generator.categories,
                    gameVersions = generator.mcVersions
                ).map { (id, slug) ->
                    slug
                }
            }
            val forgeResults = generatorsForge.mapValues { (generatorId, generator) ->
                val mcVersions = ForgeUtil.mcVersionsMap(filter = generator.mcVersions)
                println(json.encodeToString(MapSerializer(String.serializer(), MapSerializer(String.serializer(), String.serializer())), mcVersions))
                val allVersions = mcVersions.flatMap { it.value.values }

                val promos = ForgeUtil.promoMap()
                for ((keyIdentifier, version) in promos) {
                    if (allVersions.contains(version)) {
                        //TODO: add promo keys
//                        forgeBuilder.addProperty(buildProperty(keyIdentifier, version))
                    }
                }
            }

            //TODO: look up fabric versions from meta.fabricmc.net



            //TODO: write generated constants into enum fields
            val allSlugs = curseResults.map { (k, v) -> "$k/$v" }
            schemaFile.writeText(
                schemaFile.readText()
                    .replace("\"replace_with_projectnames\"",
                        allSlugs.joinToString(",") { "\"$it\"" }
                    )
                    .replace("\"replace_with_tags\"",
                        modpackConfig.tags.keys.joinToString(",") { "\"$it\"" }
                    )
            )
        }


//        val allSlugs = runBlocking {
//            val fabricSlugs = CurseClient.graphQLRequest(
//                    section = CurseSection.MODS.sectionName,
//                    categories = listOf("Fabric"),
//                    gameVersions = listOf(modpackConfig.mcVersion)
//                ).map { (id, slug) ->
//                    slug
//                }
//
//            val otherSlugs = CurseClient.graphQLRequest(
//                    section = CurseSection.MODS.sectionName,
//                    categories = emptyList(),
//                    gameVersions = listOf(modpackConfig.mcVersion)
//                ).map { (id, slug) ->
//                    slug
//                }.filter { it !in fabricSlugs }
//
//            val resourcepackSlugs = CurseClient.graphQLRequest(
//                    section = CurseSection.RESOURCE_PACKS.sectionName,
//                    categories = emptyList(),
//                    gameVersions = listOf(modpackConfig.mcVersion)
//                ).map { (id, slug) ->
//                    slug
//                }.filter { it !in fabricSlugs }
//
//            fabricSlugs.map { "fabric/$it" } + otherSlugs.map { "mods/$it" } + resourcepackSlugs.map { "resource/$it" }
//        }

//        schemaFile.writeText(
//            schemaFile.readText()
//                .replace("\"replace_with_projectnames\"",
//                    allSlugs.joinToString(",") { "\"$it\"" }
//                )
//                .replace("\"replace_with_tags\"",
//                    modpackConfig.tags.keys.joinToString(",") { "\"$it\"" }
//                )
//        )

        val jsonObj = json.parseToJsonElement(testFile.readText()).jsonObject
        testFile.writeText(
            json.encodeToString(
                JsonObject.serializer(),
                JsonObject(mapOf("\$schema" to JsonPrimitive(schemaFile.toRelativeString(File(".").absoluteFile))) + jsonObj)
            )
        )


//        val schema = buildJsonSchema {
//            property("authors", String.serializer().list, true) {
//                contents["type"] = JsonLiteral("array")
//                contents["contains"] = JsonObject(mapOf("type" to JsonLiteral("string")))
//            }
//        }
//
//        println("schema: $schema")
    }
}

