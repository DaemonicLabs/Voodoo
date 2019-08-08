package voodoo.data

sealed class TaskType(open val command: String) {
    object ImportDebug: TaskType("import_debug")

    object Build: TaskType("build")

    object Diff: TaskType("diff")

    sealed class Pack(val subCommand: String): TaskType("pack $subCommand") {
        object SKLauncher: Pack("sk")
        object MultiMC: Pack("mmc")
        object MultiMCFat: Pack("mmc-fat")
        object MultiMCStatic: Pack("mmc-static")
        object Server: Pack("server")
        object Curse: Pack("curse")
    }

    sealed class Test(val subCommand: String): TaskType("test $subCommand")  {
        object MultiMC : TaskType("mmc")
    }
}