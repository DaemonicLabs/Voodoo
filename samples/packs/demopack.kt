import kotlin.String
import voodoo.provider.CurseProvider
import voodoo.withDefaultMain

fun main(vararg args: String) = withDefaultMain(
    root = Constants.rootDir.resolve("run"),
    arguments = args
) {
    nestedPack(
        id = "demopack",
        mcVersion = "1.12.2"
    ) {
        title = "Demo Pack"
        authors = listOf("nikky")
        forge = 2768
        root = rootEntry(CurseProvider) {
            list {
                id(Mod.harvestables)
                id(Mod.bouncyCreepers)
                id(Mod.oreExcavation)
                id(Mod.r2sCore)
                id(Mod.woolySheep)
                id(Mod.metamorph)
            }
        }
    }
}
