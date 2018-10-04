import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.task
import org.gradle.plugins.ide.idea.IdeaPlugin

open class ConstantsExtension {
    var major: String? = null
    var minor: String? = null
    var patch: String? = null
    var build: String? = null
}

open class ConstantGenerator : Plugin<Project> {
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

@CacheableTask
open class GenerateConstantsTask : DefaultTask() {
    lateinit var extension: ConstantsExtension

    @OutputDirectory
    val outputFolder = ConstantGenerator.outputFolder(project)

    val folder = listOf("voodoo") + when (project.depth) {
        0 -> emptyList()
        else -> project.name.split('-')
    }
    val className = project.name
        .split("-")
        .joinToString("") {
            it.capitalize()
        } + "Constants"
    val pkg = folder.joinToString(".")

    @OutputFile
    val targetFile = outputFolder.resolve(pkg.replace('.', '/')).resolve(className + ".kt")

    init {
        group = "build"
        description = "Generates Constants"
    }

    @TaskAction
    fun createConstants() {
        println(project.name)
        val major: String = extension.major ?: project.properties["major"] as String
        val minor: String = extension.minor ?: project.properties["minor"] as String
        val patch: String = extension.patch ?: project.properties["patch"] as String

        val build: String? = extension.build ?: System.getenv("BUILD_NUMBER")

        outputFolder.mkdirs()

        val constantBuilder = TypeSpec.objectBuilder(ClassName(pkg, className))
        constantBuilder.addProperty(
            PropertySpec.builder("MAJOR_VERSION", String::class, KModifier.CONST)
                .initializer("%S", major)
                .build()
        )
        constantBuilder.addProperty(
            PropertySpec.builder("MINOR_VERSION", String::class, KModifier.CONST)
                .initializer("%S", minor)
                .build()
        )
        constantBuilder.addProperty(
            PropertySpec.builder("PATCH_VERSION", String::class, KModifier.CONST)
                .initializer("%S", patch)
                .build()
        )
        constantBuilder.addProperty(
            PropertySpec.builder("BUILD_NUMBER", Int::class, KModifier.CONST)
                .initializer("%L", build ?: -1)
                .build()
        )
        constantBuilder.addProperty(
            PropertySpec.builder("BUILD", String::class, KModifier.CONST)
                .initializer("%S", "$major.$minor.$patch-${build ?: "dev"}")
                .build()
        )

        constantBuilder.addProperty(
            PropertySpec.builder("VERSION", String::class, KModifier.CONST)
                .initializer("%S", "$major.$minor.$patch")
                .build()
        )
        constantBuilder.addProperty(
            PropertySpec.builder("FULL_VERSION", String::class, KModifier.CONST)
                .initializer("%S", "$major.$minor.$patch-${build ?: "dev"}")
                .build()
        )

        val source = FileSpec.get(pkg, constantBuilder.build())
        source.writeTo(outputFolder)
    }
}