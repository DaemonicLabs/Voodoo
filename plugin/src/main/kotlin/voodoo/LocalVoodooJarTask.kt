package voodoo

import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.kotlin.dsl.getByName
import voodoo.plugin.PluginConstants
import java.io.File

open class LocalVoodooJarTask : GradleBuild() {
    @OutputDirectory
    val outputFolder = project.buildDir.resolve("voodoo")
    @OutputFile
    val jarFile: File = outputFolder.resolve("voodoo-${PluginConstants.FULL_VERSION}.jar")

    init {
        val voodooExtension = project.extensions.getByName<VoodooExtension>("voodoo")

        group = "voodoo"
        buildFile = voodooExtension.localVoodooProjectLocation.resolve("build.gradle.kts")
        tasks = mutableListOf("voodoo:clean", "voodoo:shadowJar")
        dir = voodooExtension.localVoodooProjectLocation
        startParameter = startParameter.newInstance().apply {
            isRefreshDependencies = true
        }

        doLast {
            val shadowJarFile: File = voodooExtension.localVoodooProjectLocation.resolve("voodoo")
                .resolve("build").resolve("libs")
                .resolve("voodoo-${PluginConstants.FULL_VERSION}.jar")
            shadowJarFile.copyTo(jarFile, overwrite = true)
        }
        outputs.upToDateWhen { false }
    }
}