package voodoo.data.sk.task

data class TaskWhen(
        val `if`: TaskIf,
        val features: List<String>
)