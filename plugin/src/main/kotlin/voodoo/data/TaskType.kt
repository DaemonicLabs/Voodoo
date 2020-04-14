package voodoo.data

import voodoo.pack.*

sealed class TaskType(open val command: String) {
    object ImportDebug: TaskType("import_debug")

    object Build: TaskType("build")

    object Changelog: TaskType("changelog")

    sealed class Pack(subCommand: String): TaskType("pack $subCommand") {
        object Experimental: Pack(ExperimentalPack.id)
        object SKLauncher: Pack(SKPack.id)
        object MultiMCExp: Pack(MMCSelfupdatingPackExp.id)
        object MultiMCSk: Pack(MMCSelfupdatingPack.id)
        object MultiMCSkFat: Pack(MMCSelfupdatingFatPack.id)
        object MultiMCFat: Pack(MMCFatPack.id)
        object Server: Pack(ServerPack.id)
        object Curse: Pack(CursePack.id)
    }

    sealed class Test(subCommand: String): TaskType("test $subCommand")  {
        object MultiMC : Test("mmc")
    }
}