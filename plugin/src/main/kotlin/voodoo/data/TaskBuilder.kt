package voodoo.data

class TaskBuilder() {
    internal val tasks: MutableList<TaskType> = mutableListOf()

    fun importDebug() {
        tasks += TaskType.ImportDebug
    }

    fun build() {
        tasks += TaskType.Build
    }
    fun diff() {
        tasks += TaskType.Diff
    }
    fun pack(): PackContext {
        val packContext = PackContext(tasks)
        return packContext
    }
    fun test(): TestContext {
        val testContext = TestContext(tasks)
        return testContext
    }

    class PackContext(private val tasks: MutableList<TaskType>) {
        fun sklauncher() {
            tasks += TaskType.Pack.SKLauncher
        }
        fun multimc() {
            tasks += TaskType.Pack.MultiMC
        }
        fun multimcFat() {
            tasks += TaskType.Pack.MultiMCFat
        }
        fun multimcStatic() {
            tasks += TaskType.Pack.MultiMCStatic
        }
        fun server() {
            tasks += TaskType.Pack.Server
        }
        fun curse() {
            tasks += TaskType.Pack.Curse
        }
    }

    class TestContext(private val tasks: MutableList<TaskType>) {
        fun multimc() {
            tasks += TaskType.Test.MultiMC
        }
    }
    operator fun plus(task: TaskType): TaskBuilder {
        tasks += task
        return this
    }
}
