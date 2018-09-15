package voodoo.data.sk.task

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 */
@Serializable
data class Task(
        val type: TaskType,
        val hash: String,
        val location: String,
        val to: String,
        @Optional val size: Int = 0,
        @Optional val `when`: TaskWhen? = null,
        @Optional val userFile: Boolean = false
)