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
        @Deprecated("will be removed")
        fun sklauncher() {
            tasks += TaskType.Pack.SKLauncher
        }
        fun voodoo() {
            tasks += TaskType.Pack.VoodooPackage
        }
        @Deprecated("renamed", ReplaceWith("voodoo()"))
        fun experimental() {
            tasks += TaskType.Pack.VoodooPackage
        }
        @Deprecated("renamed", ReplaceWith("multimcSk()"))
        fun multimc() {
            tasks += TaskType.Pack.MultiMCSk
        }
        fun multimcVoodoo() {
            tasks += TaskType.Pack.MultiMCVoodoo
        }
        @Deprecated("renamed", ReplaceWith("multimcVoodoo()"))
        fun multimcExperimental() {
            tasks += TaskType.Pack.MultiMCVoodoo
        }
        @Deprecated("will be removed")
        fun multimcSk() {
            tasks += TaskType.Pack.MultiMCSk
        }
        @Deprecated("will be removed")
        fun multimcSkFat() {
            tasks += TaskType.Pack.MultiMCSkFat
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
