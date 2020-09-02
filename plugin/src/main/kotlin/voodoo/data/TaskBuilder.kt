package voodoo.data

class TaskBuilder() {
    internal val tasks: MutableList<TaskType> = mutableListOf()
    fun build() {
        tasks += TaskType.Build
    }
    fun changelog() {
        tasks += TaskType.Changelog
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
        fun voodoo() {
            tasks += TaskType.Pack.VoodooPackage
        }
        fun native() {
            tasks += TaskType.Pack.VoodooPackage
        }
        fun multimcVoodoo() {
            tasks += TaskType.Pack.MultiMCVoodoo
        }
        fun multimcFat() {
            tasks += TaskType.Pack.MultiMCFat
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
