package voodoo

import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import voodoo.plugin.PluginConstants
import voodoo.util.maven.MavenUtil
import java.io.File

open class DownloadVoodooTask : DefaultTask() {
    @OutputDirectory
    val outputFolder = project.buildDir.resolve("voodoo")
    @OutputFile
    val lastFile = outputFolder.resolve("last.txt")
    @OutputFile
    val jarFile: File = outputFolder.resolve("voodoo-${PluginConstants.FULL_VERSION}.jar")

    init {
        group = "voodoo"

//        outputs.upToDateWhen {
//            //            if(PluginConstants.JENKINS_BUILD_NUMBER < 0)
////                return@upToDateWhen false
//
//            if (jarFile.exists() && lastFile.exists()) {
//                val lastBuild = lastFile.readText().toIntOrNull() ?: run {
//                    return@upToDateWhen false
//                }
//                lastBuild == PluginConstants.JENKINS_BUILD_NUMBER
//            } else {
//                false
//            }
//        }
    }

    @TaskAction
    fun exec() {

        logger.lifecycle("download voodoo")

        val stopwatch = Stopwatch("downloadVoodooTask")
        stopwatch {
            runBlocking {
                MavenUtil.downloadArtifact(
                    stopwatch = "downloadVoodoo".watch,
                    mavenUrl = "http://maven.modmuss50.me",
                    group = "moe.nikky.voodoo",
                    artifactId = "voodoo",
                    version = PluginConstants.FULL_VERSION,
                    variant = "all",
                    outputDir = outputFolder,
                    outputFile = jarFile
                )
            }
            lastFile.writeText(PluginConstants.FULL_VERSION)
        }
        logger.info(stopwatch.toStringPretty())
    }
}
