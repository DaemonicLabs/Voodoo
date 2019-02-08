package voodoo.poet

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import kotlinx.coroutines.runBlocking
import mu.KLogging
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
import voodoo.data.curse.Section
import voodoo.forge.ForgeUtil
import voodoo.poet.generator.CurseGenerator
import voodoo.poet.generator.ForgeGenerator
import java.io.File

object Poet : KLogging() {
//    @JvmStatic
//    fun main(vararg args: String) {
//        generateAll(generatedSrcDir = File(args[0]))
//    }

    fun generateAll(
        generatedSrcDir: File, // = SharedFolders.GeneratedSrc.get(id = id),
        slugSanitizer: (String) -> String = Poet::defaultSlugSanitizer,
        curseGenerators: List<CurseGenerator> = listOf(),
        forgeGenerators: List<ForgeGenerator> = listOf()
    ): List<File> {

//    class XY
//    println("classloader is of type:" + Thread.currentThread().contextClassLoader)
//    println("classloader is of type:" + ClassLoader.getSystemClassLoader())
//    println("classloader is of type:" + XY::class.java.classLoader)
        Thread.currentThread().contextClassLoader = Poet::class.java.classLoader

        val files = runBlocking {
            // TODO: parallelize
            curseGenerators.map { generator ->
                Poet.generate(
                    name = generator.name,
                    slugIdMap = request(
                        gameVersions = generator.mcVersions.toList(),
                        section = generator.section
                    ),
                    slugSanitizer = slugSanitizer,
                    folder = generatedSrcDir
                )
            } + forgeGenerators.map { generator ->
                Poet.generateForge(
                    name = generator.name,
                    mcVersionFilters = generator.mcVersions.toList(), //generator.mcVersions.toList(),
                    folder = generatedSrcDir
                )
            }
        }
        return files

//        return runBlocking {
//            listOf(
//                Poet.generate(
//                    name = "Mods_112",
//                    slugIdMap = Poet.request112Mods(),
//                    slugSanitizer = slugSanitizer,
//                    folder = generatedSrcDir
//                ),
//                Poet.generate(
//                    name = mods,
//                    slugIdMap = Poet.requestMods(),
//                    slugSanitizer = slugSanitizer,
//                    folder = generatedSrcDir
//                ),
//                Poet.generate(
//                    name = texturePacks,
//                    slugIdMap = Poet.requestResourcePacks(),
//                    slugSanitizer = slugSanitizer,
//                    folder = generatedSrcDir
//                ),
//                Poet.generateForge("Forge", folder = generatedSrcDir)
//            )
//        }
    }

    fun defaultSlugSanitizer(slug: String) = slug
        .split('-')
        .joinToString("") {
            it.capitalize()
        }.decapitalize()

    internal fun generate(
        name: String,
        slugIdMap: Map<String, ProjectID>,
        slugSanitizer: (String) -> String,
        folder: File
    ): File {
        val idType = ClassName("voodoo.dsl", "ID")
        val objectBuilder = TypeSpec.objectBuilder(name)
        slugIdMap.entries.sortedBy { (slug, id) ->
            slug
        }.forEach { (slug, id) ->
            val projectPage = "https://minecraft.curseforge.com/projects/$slug"
            objectBuilder.addProperty(
                PropertySpec.builder(
                    slugSanitizer(slug),
//                    Int::class
                    idType
                )
//                    .addAnnotation(JvmSynthetic::class)
                    .addKdoc("@see %L\n", projectPage)
                    .mutable(false)
//                    .initializer("%T(%L)", idType, id.value)
//                    .initializer("%L", id.value)
                    .getter(
                        FunSpec.getterBuilder()
                            .addModifiers(KModifier.INLINE)
                            .addCode("return %T(%L)", idType, id.value)
                            .build()
                    )
                    .build()
            )
        }

        return save(objectBuilder.build(), name, folder)
    }

    internal suspend fun generateForge(
        name: String = "Forge",
        mcVersionFilters: List<String>? = null,
        folder: File
    ): File {
        val forgeData = runBlocking {
            ForgeUtil.deferredData.await()
        }

        fun buildProperty(identifier: String, build: Int): PropertySpec {
            return PropertySpec
                .builder(identifier, Int::class, KModifier.CONST)
                .initializer("%L", build)
                .build()
        }

        val forgeBuilder = TypeSpec.objectBuilder(name)
        val webpath =
            PropertySpec.builder("WEBPATH", String::class, KModifier.CONST).initializer("%S", forgeData.webpath).build()
        forgeBuilder.addProperty(webpath)

        val mcVersions = ForgeUtil.mcVersionsMap(filter = mcVersionFilters)
        val allNumbers = mcVersions.flatMap { it.value.values }
        for ((versionIdentifier, numbers) in mcVersions) {
            val versionBuilder = TypeSpec.objectBuilder(versionIdentifier)
            for ((buildIdentifier, number) in numbers) {
                versionBuilder.addProperty(buildProperty(buildIdentifier, number))
            }
            forgeBuilder.addType(versionBuilder.build())
        }

        val promos = ForgeUtil.promoMap()
        for ((keyIdentifier, number) in promos) {
            if(allNumbers.contains(number))
                forgeBuilder.addProperty(buildProperty(keyIdentifier, number))
        }

        return save(forgeBuilder.build(), name, folder)
    }

    private fun save(source: FileSpec, name: String, folder: File): File {
        val path = folder.apply {
            absoluteFile.parentFile.mkdirs()
        }.absoluteFile
        val targetFile = path.resolve("$name.kt")
        source.writeTo(path)
        logger.info("written to $targetFile")
        return targetFile
    }

    private fun save(type: TypeSpec, name: String, folder: File): File {
        folder.mkdirs()
        val source = FileSpec.get("", type)
        val path = folder.apply {
            absoluteFile.parentFile.mkdirs()
        }.absoluteFile
        val targetFile = path.resolve("$name.kt")
        source.writeTo(path)
        logger.info("written to $targetFile")
        return targetFile
    }

    suspend fun request(section: Section, gameVersions: List<String>? = null): Map<String, ProjectID> =
        CurseClient.graphQLRequest(
            section = section,
            gameVersions = gameVersions
        ).map { (id, slug) ->
            slug to ProjectID(id)
        }.toMap()
}