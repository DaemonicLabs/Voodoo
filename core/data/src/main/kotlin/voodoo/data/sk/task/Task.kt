package voodoo.data.sk.task

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 */
data class Task(
        val type: TaskType,
        val hash: String,
        val location: String,
        val to: String,
        val size: Int,
        val `when`: TaskWhen? = null,
        val userFile: Boolean = false
)