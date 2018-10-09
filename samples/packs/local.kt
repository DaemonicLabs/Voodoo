/* ktlint-disable no-wildcard-imports */
import voodoo.provider.LocalProvider
import voodoo.withDefaultMain

/* ktlint-enable no-wildcard-imports */

fun main(args: Array<String>) = withDefaultMain(
    root = Constants.rootDir.resolve("run").resolve("local"),
    arguments = args
) {
    nestedPack(
        id = "local",
        mcVersion = "1.12.2"
    ) {
        localDir = "local"
        root = rootEntry(LocalProvider) {
            list {
                id("correlated") {
                    fileSrc = "Correlated-1.12.2-2.1.125.jar"
                }
            }
        }
    }
}