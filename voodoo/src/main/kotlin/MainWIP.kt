import mu.KotlinLogging
import voodoo.config.Configuration
import voodoo.config.generateSchema
import voodoo.pack.MetaPack
import voodoo.pack.VersionPack
import voodoo.util.SharedFolders
import voodoo.util.json
import java.io.File

object MainWIP {
    private val logger = KotlinLogging.logger {}

    @JvmStatic
    fun main(args: Array<String>) {
        logger.info { "Hello World" }

        val rootDir = File(".").absoluteFile
        SharedFolders.RootDir.value = rootDir

        val id = "test"

        val config = Configuration.parse(rootDir = rootDir)

        val baseDir = rootDir.resolve(id)
        val metaPackFile = baseDir.resolve(MetaPack.FILENAME)
        val metaPack = json.decodeFromString(MetaPack.serializer(), metaPackFile.readText())

        rootDir.resolve("schema/modpack.schema.json").apply {
            absoluteFile.parentFile.mkdirs()
            writeText(VersionPack.generateSchema(setOf()))
        }

        val testFile = File("test.voodoo.json").absoluteFile

        val versionPack = json.decodeFromString(
            VersionPack.serializer(),
            testFile.readText()
        )

        rootDir.resolve("schema/modpack.schema.json").apply {
            absoluteFile.parentFile.mkdirs()
            writeText(VersionPack.generateSchema(config.overrides.keys))
        }

        println("modpackInput: $versionPack")

        val modpack = versionPack.flatten(rootDir = rootDir, id = id, overrides = config.overrides, metaPack = metaPack)

        println("modpack: $modpack")

//        val jsonObj = json.parseToJsonElement(testFile.readText()).jsonObject
//        testFile.writeText(
//            json.encodeToString(
//                JsonObject.serializer(),
//                JsonObject(mapOf("\$schema" to JsonPrimitive(schemaFile.toRelativeString(File(".").absoluteFile))) + jsonObj)
//            )
//        )

        //TODO generate schema autocompletions later
//        val generatorsCurse = modpackConfig.generators.filterValues { it is Generator.Curse } as Map<String, Generator.Curse>
//        val generatorsForge = modpackConfig.generators.filterValues { it is Generator.Forge } as Map<String, Generator.Forge>
//        val generatorsFabric = modpackConfig.generators.filterValues { it is Generator.Fabric } as Map<String, Generator.Fabric>
//
//        runBlocking {
//            val curseResults = generatorsCurse.mapValues { (generatorId, generator) ->
//                CurseClient.graphQLRequest(
//                    section = generator.section.sectionName,
//                    categories = generator.categories,
//                    gameVersions = generator.mcVersions
//                ).map { (id, slug) ->
//                    slug
//                }
//            }
//            val forgeResults = generatorsForge.mapValues { (generatorId, generator) ->
//                val mcVersions = ForgeUtil.mcVersionsMap(filter = generator.mcVersions)
//                println(json.encodeToString(MapSerializer(String.serializer(), MapSerializer(String.serializer(), String.serializer())), mcVersions))
//                val allVersions = mcVersions.flatMap { it.value.values }
//
//                val promos = ForgeUtil.promoMapSanitized()
//                for ((keyIdentifier, version) in promos) {
//                    if (allVersions.contains(version)) {
//                        //TODO: add promo keys
////                        forgeBuilder.addProperty(buildProperty(keyIdentifier, version))
//                    }
//                }
//            }
//
//            //TODO: look up fabric versions from meta.fabricmc.net
//
//
//
//            //TODO: write generated constants into enum fields
//            val allSlugs = curseResults.map { (k, v) -> "$k/$v" }
//            schemaFile.writeText(
//                schemaFile.readText()
//                    .replace("\"replace_with_projectnames\"",
//                        allSlugs.joinToString(",") { "\"$it\"" }
//                    )
//                    .replace("\"replace_with_tags\"",
//                        modpackConfig.tags.keys.joinToString(",") { "\"$it\"" }
//                    )
//            )
//        }


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

