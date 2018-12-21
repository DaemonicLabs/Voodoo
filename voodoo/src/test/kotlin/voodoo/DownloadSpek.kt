//package voodoo
//
//import kotlinx.coroutines.runBlocking
//import org.spekframework.spek2.Spek
//import org.spekframework.spek2.style.specification.describe
//import voodoo.util.download
//import java.io.File
//
//object DownloadSpek : Spek({
//    describe("tests") {
//        val rootFolder by memoized {
//            File("run").resolve("test").resolve("downloadspek").absoluteFile.apply {
//                deleteRecursively()
//                mkdirs()
//            }
//        }
//
//        val targetFile by memoized {
//            rootFolder.resolve("out.jar")
//        }
//
//        it("download file") {
//            runBlocking {
//                rootFolder.resolve("botania.jar").download(
//                    "https://edge.forgecdn.net/files/2630/989/Botania%20r1.10-357.jar",
//                    rootFolder.resolve("cache")
//                )
//                rootFolder.resolve("bedrockores.jar").download(
//                    "https://edge.forgecdn.net/files/2602/707/Bedrock Ores-MC1.12-1.2.7.42.jar\n",
//                    rootFolder.resolve("cache")
//                )
//            }
//        }
//    }
//})