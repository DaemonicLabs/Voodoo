import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.provideDelegate
import java.io.File

open class GenerateConstantsTask : DefaultTask() {
    val folder = listOf("voodoo") + when(project.depth) {
        0 -> emptyList()
        else -> project.name.split('-')
    }
    val className = project.name
        .split("-")
        .joinToString("") {
            it.capitalize()
        } + "Constants"
    val pkg = folder.joinToString(".")

    @OutputDirectory
    val outputFolder = project.buildDir.resolve("generated-src")

    @OutputFile
    val targetFile = outputFolder.resolve(pkg.replace('.', '/')).resolve(className + ".kt")

    init {
        group = "build"
        description = "Generates Constants"
    }

    @TaskAction
    fun createConstants() {
        println(project.name)
        val major: String by project
        val minor: String by project
        val patch: String by project

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
        val build: String? = System.getenv("BUILD_NUMBER")
        constantBuilder.addProperty(
            PropertySpec.builder("BUILD_NUMBER", Int::class, KModifier.CONST)
                .initializer("%L", build ?: -1 )
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