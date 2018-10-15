import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
open class GenerateConstantsTask : DefaultTask() {
    lateinit var extension: ConstantsExtension // = project.extensions.getByName<ConstantsExtension>("constants")

    @OutputDirectory
    val outputFolder = GeneratorPlugin.outputFolder(project)

    init {
        group = "build"
        description = "Generates Constants"
    }

    @TaskAction
    fun createConstants() {
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
}