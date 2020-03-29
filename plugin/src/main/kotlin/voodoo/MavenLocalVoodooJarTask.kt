package voodoo

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.kotlin.dsl.getByName
import voodoo.plugin.PluginConstants
import voodoo.util.maven.MavenUtil
import java.io.File

open class MavenLocalVoodooJarTask : GradleBuild() {
    @OutputDirectory
    val outputFolder = project.buildDir.resolve("voodoo")
    @OutputFile
    val jarFile: File = outputFolder.resolve("voodoo-${PluginConstants.FULL_VERSION}.jar")

    init {
        val voodooExtension = project.extensions.getByName<VoodooExtension>("voodoo")

        group = "voodoo"
        val gradleBuildVile = voodooExtension.localVoodooProjectLocation?.resolve("build.gradle.kts")

        if(gradleBuildVile?.exists() == true) {
            buildFile = gradleBuildVile
            tasks = mutableListOf("voodoo:clean", "voodoo:publishToMavenLocal")
            dir = gradleBuildVile.parentFile
            startParameter = startParameter.newInstance().apply {
                isRefreshDependencies = true
            }
        }

        doLast {
            val shadowJarFile: File = MavenUtil.localMavenFile(
                group = PluginConstants.MAVEN_GROUP,
                artifactId = "voodoo",
                version = PluginConstants.FULL_VERSION,
                classifier = PluginConstants.MAVEN_SHADOW_CLASSIFIER
            )
            shadowJarFile.copyTo(jarFile, overwrite = true)
        }
        outputs.upToDateWhen { false }
    }
}