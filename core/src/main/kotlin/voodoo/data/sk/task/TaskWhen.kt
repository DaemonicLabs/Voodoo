package voodoo.data.sk.task

import kotlinx.serialization.Serializable

@Serializable
data class TaskWhen(
    val `if`: TaskIf,
    val features: List<String>
)