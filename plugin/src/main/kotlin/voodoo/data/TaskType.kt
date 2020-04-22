package voodoo.data

import voodoo.pack.*

//TODO: unify with code in :voodoo
sealed class TaskType(open val command: String) {
    object Build: TaskType("build")

    object Changelog: TaskType("changelog")

    sealed class Pack(subCommand: String): TaskType("pack $subCommand") {
        object VoodooPackage: Pack(VoodooPackager.id)
        object MultiMCVoodoo: Pack(MMCSelfupdatingPackVoodoo.id)
        @Deprecated("will be removed")
        object SKLauncher: Pack(SKPack.id)
        @Deprecated("will be removed")
        object MultiMCSk: Pack(MMCSelfupdatingPackSk.id)
        @Deprecated("will be removed")
        object MultiMCSkFat: Pack(MMCSelfupdatingFatPackSk.id)
        object MultiMCFat: Pack(MMCFatPack.id)
        object Server: Pack(ServerPack.id)
        object Curse: Pack(CursePack.id)
    }

    sealed class Test(subCommand: String): TaskType("test $subCommand")  {
        object MultiMC : Test("mmc")
    }
}