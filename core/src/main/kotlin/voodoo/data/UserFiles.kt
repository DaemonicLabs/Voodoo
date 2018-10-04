package voodoo.data

import kotlinx.serialization.Serializable

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */

@Serializable
data class UserFiles(
    var include: List<String> = listOf("options.txt", "optionsshaders.txt"),
    var exclude: List<String> = emptyList()
)