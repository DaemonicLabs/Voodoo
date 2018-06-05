package voodoo.hex.sk

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 * @version 1.0
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

data class TaskWhen(
        val `if`: IfTask,
        val features: List<String>
)

enum class IfTask {
    requireAny
}

enum class TaskType {
    file
}