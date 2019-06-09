package voodoo.poet

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import kotlinx.coroutines.runBlocking
import mu.KLogging
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
import voodoo.forge.ForgeUtil
import voodoo.poet.generator.CurseGenerator
import voodoo.poet.generator.ForgeGenerator
import java.io.File

object Poet : KLogging() {
    // for generating code for tests only
    @JvmStatic
    fun main(vararg args: String) {
        generateAll(
            generatedSrcDir = File(args[0]),
            curseGenerators = listOf(CurseGenerator("Mod", "Mods")),
            forgeGenerators = listOf(ForgeGenerator("Forge"))
        )
    }

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

        return runBlocking {
            // TODO: parallelize

            val curseFiles = curseGenerators.map { generatedSrcDir.resolve("${it.name}.kt") }
            val runCurseGenerator = curseFiles.any { !it.exists() }
            val generatedCurseFiles = if (runCurseGenerator) {
                val results: Map<String, MutableMap<String, ProjectID>> = curseGenerators.associate {
                    it.name to mutableMapOf<String, ProjectID>()
                }
                CurseClient.scanAllProjects<Unit> { addon ->
                    curseGenerators.forEach { generator ->
                        if (
                            (addon.gameVersionLatestFiles.isEmpty() || addon.gameVersionLatestFiles.any { generator.mcVersions.contains(it.gameVersion) }) &&
                            addon.categorySection.name == generator.section
                        ) {
                            results.getValue(generator.name)[addon.slug] = addon.id
                            logger.info("added addon: ${addon.slug}")
                        }
                    }
                }

                curseGenerators.map { generator ->
                    Poet.generate(
                        name = generator.name,
                        slugIdMap = results.getOrDefault(generator.name, mutableMapOf()),
                        slugSanitizer = slugSanitizer,
                        folder = generatedSrcDir
                    )
                }
            } else {
                curseFiles
            }

            val forgeFiles = forgeGenerators.map { generatedSrcDir.resolve("${it.name}.kt") }
            val runForgeGenerator = forgeFiles.any { !it.exists() }
            val generatedForgeFiles = if (runForgeGenerator) {
                forgeGenerators.map { generator ->
                    Poet.generateForge(
                        name = generator.name,
                        mcVersionFilters = generator.mcVersions.toList(), // generator.mcVersions.toList(),
                        folder = generatedSrcDir
                    )
                }
            } else {
                forgeFiles
            }

            generatedCurseFiles + generatedForgeFiles
        }
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
        val targetFile = folder.resolve("$name.kt")
        if (targetFile.exists()) {
            logger.info("skipping generation of $targetFile")
            return targetFile
        }

        val idType = ProjectID::class.asTypeName()
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

        return save(objectBuilder.build(), targetFile)
    }

    internal suspend fun generateForge(
        name: String = "Forge",
        mcVersionFilters: List<String>? = null,
        folder: File
    ): File {
        val targetFile = folder.resolve("$name.kt")
        if (targetFile.exists()) {
            logger.info("skipping generation of $targetFile")
            return targetFile
        }

        fun buildProperty(identifier: String, version: String): PropertySpec {
            return PropertySpec
                .builder(
                    identifier.replace('-', '_').replace('.', '_'),
                    String::class,
                    KModifier.CONST
                )
                .initializer("%S", version)
                .build()
        }

        val forgeBuilder = TypeSpec.objectBuilder(name)

        val mcVersions = ForgeUtil.mcVersionsMap(filter = mcVersionFilters)
        val allVersions = mcVersions.flatMap { it.value.values }
        mcVersions.forEach { (versionIdentifier, numbers) ->
            val versionBuilder = TypeSpec.objectBuilder(versionIdentifier)
            for ((buildIdentifier, version) in numbers) {
                versionBuilder.addProperty(buildProperty(buildIdentifier, version))
            }
            forgeBuilder.addType(versionBuilder.build())
        }

        val promos = ForgeUtil.promoMap()
        for ((keyIdentifier, version) in promos) {
            if (allVersions.contains(version)) {
                forgeBuilder.addProperty(buildProperty(keyIdentifier, version))
            }
        }

        return save(forgeBuilder.build(), targetFile)
    }

    private fun save(type: TypeSpec, targetFile: File): File {
        val source = FileSpec.get("", type)
        val targetFolder = targetFile.absoluteFile.parentFile.apply { mkdirs() }
        source.writeTo(targetFolder)
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

    suspend fun request(section: String, gameVersions: List<String>? = null): Map<String, ProjectID> =
        CurseClient.scanAllProjects { addon ->
            if (
                addon.gameVersionLatestFiles.any { gameVersions?.contains(it.gameVersion) == true } &&
                addon.categorySection.name == section
            ) {
                addon.slug to addon.id
            } else null
        }.filterNotNull().toMap()

}