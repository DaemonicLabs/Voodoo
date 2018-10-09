import voodoo.data.nested.NestedPack
import voodoo.property

@VoodooDSL
data class ModpackWrapper(
    val pack: NestedPack
) {
    var title by property(pack::title)
    var version by property(pack::version)
    var icon by property(pack::icon)
    var authors by property(pack::authors)
    var forge by property(pack::forge)
    var userFiles by property(pack::userFiles)
    var launch by property(pack::launch)
    var root by property(pack::root)
    var localDir by property(pack::localDir)
    var sourceDir by property(pack::sourceDir)
}
