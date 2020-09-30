import com.charleskorn.kaml.Yaml
import com.github.ricky12awesome.jss.dsl.ExperimentalJsonSchemaDSL
import com.github.ricky12awesome.jss.dsl.buildJsonSchema
import com.github.ricky12awesome.jss.stringifyToSchema
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonLiteral
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.map
import mu.KotlinLogging
import voodoo.data.nested.NestedPack
import voodoo.util.json
import java.io.File

object Main {
    private val logger = KotlinLogging.logger {}
    @OptIn(ExperimentalJsonSchemaDSL::class)
    @JvmStatic
    fun main(args: Array<String>) {
        logger.info { "Hello World" }

        val rootDir = File(".").absoluteFile
        val id = "test"


        MapSerializer(String.serializer(), String.serializer())

        val modpackConfig  = Yaml.default.parse(
            ModpackPlain.serializer(),
            File("test.voodoo.yml").readText()
        )

        println("modpack: $modpackConfig")

        // TODO: generate json later
        // debug
//        val jsonObj = json.toJson(NestedPack.serializer(), scriptEnv.pack) as JsonObject
//        rootDir.resolve(id).resolve("$id.nested.pack.json").writeText(
//            json.stringify(JsonObject.serializer(), JsonObject(mapOf("\$schema" to JsonLiteral(scriptEnv.pack.`$schema`)) + jsonObj))
//        )
//        val schemaFile = rootDir.resolve("schema/modpack.schema.json")
//        schemaFile.absoluteFile.parentFile.mkdirs()
//        schemaFile.writeText(
//            json.stringifyToSchema(ModpackPlain.serializer())
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

