package voodoo.util

import kotlinx.serialization.json.JSON

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 */
val json = JSON(indented = true, unquoted = true)

inline val <reified T: Any> T.toJson: String
    get() = json.stringify(this)