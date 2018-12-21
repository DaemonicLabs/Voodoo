package plugin

import ConstantsExtension
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.task
import org.gradle.plugins.ide.idea.IdeaPlugin

open class GeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {

            val folder = outputFolder(project)
            project.plugins.withType(IdeaPlugin::class.java) {
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
            val generateConstants = task<GenerateConstantsTask>("generateConstants") {}

        }
        project.afterEvaluate {
//            val constExtension = extensions.getByType<ConstantsExtension>()
//            createConstants(constExtension)
        }
    }

    fun Project.createConstants(extension: ConstantsExtension) {
        val outputFolder = GeneratorPlugin.outputFolder(project)
        extension.files.forEach { builder ->

            val constantBuilder =
                TypeSpec.objectBuilder(ClassName(builder.pkg, builder.className))

            builder.fields.forEach { (key, value) ->
                when (value) {
                    is String -> {
                        constantBuilder.addProperty(
                            PropertySpec.builder(
                                key,
                                String::class,
                                KModifier.CONST
                            )
                                .initializer("%S", value)
                                .build()
                        )
                    }
                    is Int -> {
                        constantBuilder.addProperty(
                            PropertySpec.builder(
                                key,
                                Int::class,
                                KModifier.CONST
                            )
                                .initializer("%L", value)
                                .build()
                        )
                    }
                }
            }

            val source = FileSpec.get(builder.pkg, constantBuilder.build())
            source.writeTo(outputFolder)
        }
    }

    companion object {
        fun outputFolder(project: Project) = project.buildDir.resolve("generated-src")
    }
}

