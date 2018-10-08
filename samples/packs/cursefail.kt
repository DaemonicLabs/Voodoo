/* ktlint-disable no-wildcard-imports */
import voodoo.*
import voodoo.data.nested.*
import voodoo.provider.*
import java.io.File
/* ktlint-enable no-wildcard-imports */

fun main(args: Array<String>) = withDefaultMain(
    root = Constants.rootDir.resolve("run").resolve("cursefail"),
    arguments = args
) {
    NestedPack(
        id = "cursefail",
        mcVersion = "1.12.2",
        root = rootEntry(CurseProvider) {
            list {
                id(Mod.electroblobsWizardry)
                id(Mod.botania)
                id(Mod.betterBuildersWands)
                id(Mod.bibliocraft)
                id(Mod.toastControl)
            }
        }
    )
}