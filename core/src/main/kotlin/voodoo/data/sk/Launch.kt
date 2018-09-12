package voodoo.data.sk

import blue.endless.jankson.JsonObject
import kotlinx.serialization.Serializable
import voodoo.getList

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */
@Serializable
data class Launch(
        var flags: List<String> = listOf("-Dfml.ignoreInvalidMinecraftCertificates=true")
) {
    companion object {
        fun fromJson(jsonObject: JsonObject) : Launch {
            return with(Launch()) {
                Launch(
                        flags = jsonObject.getList("flags")?: flags
                )
            }
        }
    }
}