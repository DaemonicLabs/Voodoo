package voodoo.data

import blue.endless.jankson.JsonObject
import mu.KLogging
import voodoo.getList

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
data class UserFiles(
        var include: List<String> = listOf("options.txt", "optionsshaders.txt"),
        var exclude: List<String> = emptyList()
) {
    companion object : KLogging() {
        fun fromJson(jsonObj: JsonObject): UserFiles {
            return with(UserFiles()) {
                UserFiles(
                        include = jsonObj.getList("include") ?: include,
                        exclude = jsonObj.getList("exclude") ?: exclude
                )
            }
        }
    }
}