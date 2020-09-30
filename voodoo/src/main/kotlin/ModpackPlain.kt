import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

enum class ModloaderType {
    FORGE, FABRIC
}

@Serializable
data class ModpackPlain(
    val title: String,
    val authors: List<String> = listOf(),
    val version: String,
    val icon: String,
    val mcVersion: String,
    val modloader: ModloaderType,
    @JsonSchema.Description(["look up versions for fabric and forge here: TODO()"]) // TODO: add url and webservice to generate
    val modloaderVersion: String,
    @JsonSchema.Description(["url to upload location of \$modpackId.json"])
    val selfupdateUrl: String,
    val tags: Map<String, PlainEntry>,
    val mods: Map<String, PlainEntry>
) {
    @Required
    var `$schema` = "./modpack.schema.json"
}

@Serializable
data class PlainEntry(
    val type: String? = null,
    val id: String
)
