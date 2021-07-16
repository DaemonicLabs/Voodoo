package voodoo.config

import com.github.ricky12awesome.jss.buildJsonSchema
import kotlinx.serialization.json.*
import mu.KotlinLogging
import voodoo.pack.FileEntry
import voodoo.pack.Modpack

private val logger = KotlinLogging.logger {}
//
//fun NestedPack.Companion.generateSchema() = json.encodeToSchema(serializer())
//    .replace("\"replace_with_curseforge_projects\"",
//        Autocompletions.curseforge.keys.joinToString(",") { "\"$it\"" }
//    )
//    .replace("\"replace_with_forge_versions\"",
//        Autocompletions.forge.keys.joinToString(",") { "\"$it\"" }
//    )
//    .replace("\"replace_with_fabric_intermediaries\"",
//        Autocompletions.fabricIntermediaries.keys.joinToString(",") { "\"$it\"" }
//    )
//    .replace("\"replace_with_fabric_loaders\"",
//        Autocompletions.fabricLoaders.keys.joinToString(",") { "\"$it\"" }
//    )
//    .replace("\"replace_with_fabric_installers\"",
//        Autocompletions.fabricInstallers.keys.joinToString(",") { "\"$it\"" }
//    )

fun Modpack.Companion.generateSchema(overridesKeys: Set<String>): String {
    val fileEntrySchemaObject = buildJsonSchema(FileEntry.serializer(), generateDefinitions = true)

    val fileEntryRef: String = fileEntrySchemaObject["\$ref"]!!.jsonPrimitive.content
    val fileEntryDefinitions = fileEntrySchemaObject["definitions"]!!.jsonObject

    val schema = buildJsonSchema(serializer(), generateDefinitions = true).toMutableMap()
    val definitions = schema["definitions"]!!.jsonObject.toMutableMap()
    val oldFileEntriesMap = definitions["FileEntryList"]!!.jsonObject
    val fileEntryArrayId = definitions["FileEntryList"]!!.jsonObject["additionalProperties"]!!.jsonObject["\$ref"]!!.jsonPrimitive
        .content.substringAfter("#/definitions/")
    val fileEntryOrStringId = definitions[fileEntryArrayId]!!.jsonObject["items"]!!.jsonObject["\$ref"]!!.jsonPrimitive
        .content.substringAfter("#/definitions/")

    definitions["FileEntryList"] = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            //TODO: add all ovverride keys here
            overridesKeys.forEach { key ->
                put(key, oldFileEntriesMap["additionalProperties"]!!.jsonObject)
            }
        }
        put("additionalProperties", false)
    }

    definitions[fileEntryOrStringId] = buildJsonObject {
        putJsonArray("oneOf") {
            val oneOfOverrides = overridesKeys.joinToString("|", "(", ")") { """(\Q$it\E)""" }
            val overridesRegex = "$oneOfOverrides,)*$oneOfOverrides"
            addJsonObject {
                put("type", "string")
                putJsonArray("oneOf") {
                    addJsonObject {
                        val group = """\w+"""
                        val slug = """(\w+-)*\w+"""
                        put("pattern", """^curse(:($oneOfOverrides,)*$oneOfOverrides)?=$group/$slug$""")
                    }
                    addJsonObject {
                        put("pattern", """^jenkins(:($oneOfOverrides,)*$oneOfOverrides)?=""")
                    }
                    addJsonObject {
                        put("pattern", """^direct(:($oneOfOverrides,)*$oneOfOverrides)?=""")
                    }
                }
            }
            addJsonObject {
                put("\$ref", fileEntryRef)
            }
        }
    }
    definitions.putAll(fileEntryDefinitions)

    schema["definitions"] = JsonObject(definitions)

    return voodoo.util.json.encodeToString(JsonObject.serializer(), JsonObject(schema))
        .replace("\"replace_with_overrides\"",
            overridesKeys.joinToString(",") { "\"${it}\"" }
        )
        .replace("\"replace_with_curseforge_projects\"",
            Autocompletions.curseforge.keys.joinToString(",") { "\"$it\"" }
        )
        .replace("\"replace_with_forge_versions\"",
            Autocompletions.forge.keys.joinToString(",") { "\"$it\"" }
        )
        .replace("\"replace_with_fabric_intermediaries\"",
            Autocompletions.fabricIntermediaries.keys.joinToString(",") { "\"$it\"" }
        )
        .replace("\"replace_with_fabric_loaders\"",
            Autocompletions.fabricLoaders.keys.joinToString(",") { "\"$it\"" }
        )
        .replace("\"replace_with_fabric_installers\"",
            Autocompletions.fabricInstallers.keys.joinToString(",") { "\"$it\"" }
        )
}