package voodoo.data

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
data class UserFiles(
        var include: List<String> = listOf("options.txt", "optionsshaders.txt"),
        var exclude: List<String> = emptyList()
)