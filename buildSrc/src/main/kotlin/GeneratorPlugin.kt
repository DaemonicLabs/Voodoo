import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.task
import org.gradle.plugins.ide.idea.IdeaPlugin

open class GeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            val folder = outputFolder(project)
            project.getPlugins().withType(IdeaPlugin::class.java) {
                model.apply {
                    module {
                        generatedSourceDirs.add(folder)
                    }
                }
            }

            project.extensions.configure<SourceSetContainer> {
                this.maybeCreate("main").allSource.srcDir(folder)
            }

            val constExtension = extensions.create("constants", ConstantsExtension::class.java)
            val generateConstants = task<GenerateConstantsTask>("generateConstants") {
                extension = constExtension
            }

//            tasks.findByName("compileKotlin")
//                ?.dependsOn(generateConstants)

        }
    }

    companion object {
        fun outputFolder(project: Project) = project.buildDir.resolve("generated-src")
    }
}

