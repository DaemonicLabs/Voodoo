package voodoo.data

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */

@Serializable
data class UserFiles(
    @Optional var include: List<String> = listOf("options.txt", "optionsshaders.txt"),
    @Optional var exclude: List<String> = emptyList()
)