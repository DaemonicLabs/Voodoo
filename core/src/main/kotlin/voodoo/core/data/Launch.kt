package voodoo.core.data

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 * @version 1.0
 */
data class Launch(
        var flags: List<String> = listOf("-Dfml.ignoreInvalidMinecraftCertificates=true")
)