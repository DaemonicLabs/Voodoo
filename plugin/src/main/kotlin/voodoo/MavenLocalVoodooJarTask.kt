package voodoo

import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.kotlin.dsl.getByName
import voodoo.plugin.GeneratedConstants
import voodoo.util.maven.MavenUtil
import java.io.File

open class MavenLocalVoodooJarTask : GradleBuild() {
    @OutputDirectory
    val outputFolder = project.buildDir.resolve("voodoo")
    @OutputFile
    val jarFile: File = outputFolder.resolve("voodoo-${GeneratedConstants.FULL_VERSION}.jar")

    init {
        val voodooExtension = project.extensions.getByName<VoodooExtension>("voodoo")

        group = "voodoo"
        val gradleBuildVile = voodooExtension.localVoodooProjectLocation?.resolve("build.gradle.kts").takeIf { voodooExtension.buildLocal }

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
                group = GeneratedConstants.MAVEN_GROUP,
                artifactId = "voodoo-main",
                version = GeneratedConstants.FULL_VERSION,
                classifier = GeneratedConstants.MAVEN_SHADOW_CLASSIFIER
            )
            shadowJarFile.copyTo(jarFile, overwrite = true)
        }
        outputs.upToDateWhen { false }
    }
}