package voodoo.poet

import com.squareup.kotlinpoet.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import mu.KLogging
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
import voodoo.forge.ForgeUtil
import voodoo.poet.generator.CurseGenerator
import voodoo.poet.generator.CurseSection
import voodoo.poet.generator.ForgeGenerator
import voodoo.util.SharedFolders
import voodoo.util.json
import java.io.File

object Poet : KLogging() {
    // for generating code for tests only
    @JvmStatic
    fun main(vararg args: String) {
        SharedFolders.RootDir.value = File(args[0])
        generateAll(
            generatedSrcDir = File(args[1]),
            curseGenerators = listOf(CurseGenerator("Mod", CurseSection.MODS)),
            forgeGenerators = listOf(ForgeGenerator("Forge"))
        )
    }

    fun generateAll(
        generatedSrcDir: File, // = SharedFolders.GeneratedSrc.get(id = id),
        curseGenerators: List<CurseGenerator> = listOf(),
        forgeGenerators: List<ForgeGenerator> = listOf()
    ): List<File> {

//    class XY
//    println("classloader is of type:" + Thread.currentThread().contextClassLoader)
//    println("classloader is of type:" + ClassLoader.getSystemClassLoader())
//    println("classloader is of type:" + XY::class.java.classLoader)

//        Thread.currentThread().contextClassLoader = Poet::class.java.classLoader

        val files = runBlocking {
            // TODO: parallelize
            curseGenerators.map { generator ->
                Poet.generate(
                    name = generator.name,
                    slugIdMap = requestSlugIdMap(
                        gameVersions = generator.mcVersions.toList(),
                        section = generator.section.sectionName
                    ),
                    slugSanitizer = generator.slugSanitizer,
                    folder = generatedSrcDir,
                    section = generator.section,
                    gameVersions = generator.mcVersions.toList()
                )
            } + forgeGenerators.map { generator ->
                Poet.generateForge(
                    name = generator.name,
                    mcVersionFilters = generator.mcVersions.toList(), // generator.mcVersions.toList(),
                    folder = generatedSrcDir
                )
            }
        }
        return files
    }

    fun defaultSlugSanitizer(slug: String) = slug
        .split('-')
        .joinToString("") {
            it.capitalize()
        }.decapitalize()

    fun generate(
        name: String,
        slugIdMap: Map<String, ProjectID>,
        slugSanitizer: (String) -> String,
        folder: File,
        section: CurseSection,
        gameVersions: List<String>
    ): File {
        //  write out json to lookup slugs by name later
        val curseSlugsFile = SharedFolders.BuildCache.get().resolve("curseSlugs.json")
        val allSlugIdMap: Map<ProjectID, String> = if(curseSlugsFile.exists()) {
            json.parse(MapSerializer(ProjectID, String.serializer()), curseSlugsFile.readText())
        } else {
            mapOf()
        } + slugIdMap.map { (slug, id) ->
            id to slug
        }
        curseSlugsFile.parentFile.mkdirs()
        curseSlugsFile.createNewFile()
        curseSlugsFile.writeText(json.stringify(MapSerializer(ProjectID, String.serializer()), allSlugIdMap))


        val targetFile = folder.resolve("$name.kt")
        if (targetFile.exists()) {
            logger.info("skipping generation of $targetFile")
            logger.info("file size: ${targetFile.length() / 1024.0} MB")
            return targetFile
        }

        val idType = ProjectID::class.asTypeName()
        val objectBuilder = TypeSpec.objectBuilder(name)
        objectBuilder.addKdoc(CodeBlock.of("%L \n", "${section.sectionName} generated from mc versions: $gameVersions"))
        objectBuilder.addKdoc("${section.sectionName} generated from mc versions: $gameVersions")
        slugIdMap.entries.sortedBy { (slug, id) ->
            slug
        }.forEach { (slug, id) ->
            val projectPage = when (section) {
                CurseSection.MODS -> "https://www.curseforge.com/minecraft/mc-mods/$slug"
                CurseSection.TEXTURE_PACKS -> "https://www.curseforge.com/minecraft/texture-packs/$slug"
            }
            objectBuilder.addProperty(
                PropertySpec.builder(
                    slugSanitizer(slug),
                    idType
                )
//                    .addKdoc(" %L\n", projectPage)
                    .mutable(false)
                    .getter(
                        FunSpec.getterBuilder()
                            .addModifiers(KModifier.INLINE)
                            .addCode("return %T(%L) \n%L\n", idType, id.value, "//·$projectPage")
                            .build()
                    )
                    .build()
            )
        }

        return save(objectBuilder.build(), targetFile)
    }

    suspend fun generateForge(
        name: String = "Forge",
        mcVersionFilters: List<String>? = null,
        folder: File
    ): File {
        val targetFile = folder.resolve("$name.kt")
        if (targetFile.exists()) {
            logger.info("skipping generation of $targetFile")
            logger.info("file size: ${targetFile.length() / 1024.0} MB")
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

    suspend fun requestSlugIdMap(section: String, gameVersions: List<String>? = null): Map<String, ProjectID> =
        CurseClient.graphQLRequest(
            section = section,
            gameVersions = gameVersions
        ).map { (id, slug) ->
            slug to id
        }.toMap()
}