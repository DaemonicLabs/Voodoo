package voodoo.data.sk

import kotlinx.serialization.Serializable

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */
@Serializable
data class Launch(
    var flags: List<String> = listOf("-Dfml.ignoreInvalidMinecraftCertificates=true")
)