package voodoo.pack

import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import voodoo.data.PackOptions
import voodoo.util.json
import java.io.File

@Serializable
data class MetaPack (
    @Required
    @SerialName("\$schema")
    @JsonSchema.NoDefinition
    val schema: String = defaultSchema,
    val title: String? = null,
    val authors: List<String> = listOf(),
    val icon: String = "icon.png",
    val packConfig: PackConfig = PackConfig(),
    // upload location //TODO: ensure this upload path is unique (or append $id), maybe grab baseUrl from config.json ?
    var uploadBaseUrl: String,
) {
    companion object {
        const val FILENAME: String = "modpack.meta.json"
        const val defaultSchema = "../schema/metaPack.schema.json"
    }

    fun save(baseDir: File): File {
        return baseDir.resolve(FILENAME).also  { file ->
            file.writeText(json.encodeToString(serializer(), this))
        }
    }
}