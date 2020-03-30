package voodoo.data

sealed class TaskType(open val command: String) {
    object ImportDebug: TaskType("import_debug")

    object Build: TaskType("build")

    object Diff: TaskType("diff")

    sealed class Pack(subCommand: String): TaskType("pack $subCommand") {
        object SKLauncher: Pack("sk")
        object MultiMCSk: Pack("mmc-sk")
        object MultiMCSkFat: Pack("mmc-sk-fat")
        object MultiMCFat: Pack("mmc-fat")
        object Server: Pack("server")
        object Curse: Pack("curse")
    }

    sealed class Test(subCommand: String): TaskType("test $subCommand")  {
        object MultiMC : Test("mmc")
    }
}