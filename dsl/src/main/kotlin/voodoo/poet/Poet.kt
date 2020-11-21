package voodoo.poet

import com.squareup.kotlinpoet.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import mu.KLogging
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
import voodoo.fabric.InstallerVersion
import voodoo.fabric.IntermediaryVersion
import voodoo.fabric.LoaderVersion
import voodoo.fabric.FabricUtil
import voodoo.forge.ForgeUtil
import voodoo.poet.generator.CurseGenerator
import voodoo.poet.generator.CurseSection
import voodoo.poet.generator.FabricGenerator
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
            curseGenerators = listOf(CurseGenerator("Mod", CurseSection.MODS, mcVersions = listOf("1.12.2"))),
            forgeGenerators = listOf(ForgeGenerator("Forge")),
            fabricGenerators = listOf(FabricGenerator("Fabric", stable = true))
        )
    }

    fun generateAll(
        generatedSrcDir: File, // = SharedFolders.GeneratedSrc.get(id = id),
        curseGenerators: List<CurseGenerator> = listOf(),
        forgeGenerators: List<ForgeGenerator> = listOf(),
        fabricGenerators: List<FabricGenerator> = listOf()
    ): List<File> {

//    class XY
//    println("classloader is of type:" + Thread.currentThread().contextClassLoader)
//    println("classloader is of type:" + ClassLoader.getSystemClassLoader())
//    println("classloader is of type:" + XY::class.java.classLoader)

//        Thread.currentThread().contextClassLoader = Poet::class.java.classLoader

        val files = runBlocking {
            // TODO: parallelize
            curseGenerators.map { generator ->
                Poet.generateCurseforgeKt(
                    name = generator.name,
                    slugIdMap = requestSlugIdMap(
                        gameVersions = generator.mcVersions.toList(),
                        categories = generator.categories,
                        section = generator.section.sectionName
                    ),
                    slugSanitizer = generator.slugSanitizer,
                    folder = generatedSrcDir,
                    section = generator.section,
                    gameVersions = generator.mcVersions.toList()
                )
            } + forgeGenerators.map { generator ->
                Poet.generateForgeKt(
                    name = generator.name,
                    mcVersionFilters = generator.mcVersions.toList(), // generator.mcVersions.toList(),
                    folder = generatedSrcDir
                )
            } + fabricGenerators.map { generator ->
                Poet.generateFabricKt(
                    name = generator.name,
                    mcVersionFilters = generator.mcVersions.toList(), // generator.mcVersions.toList(),
                    folder = generatedSrcDir
                )
            }
        }
        return files
    }

    fun defaultSlugSanitizer(slug: String) = slug
        .replace('.', '_')
        .split('-')
        .joinToString("") {
            it.capitalize()
        }.decapitalize()

    suspend fun generateCurseforgeAutocomplete(
        categories: List<String> = emptyList(),
        section: CurseSection,
        mcVersions: List<String>
    ) : Map<String, String> {
        val slugIdMap = requestSlugIdMap(
            gameVersions = mcVersions.toList(),
            categories = categories,
            section = section.sectionName
        )

        return slugIdMap.mapValues { (_, id) ->
            id.toString()
        }.toSortedMap()
    }

    suspend fun generateForgeAutocomplete(
        mcVersionFilter: List<String>
    ) : Map<String, String> {
        val mcVersionsMap = ForgeUtil.mcVersionsMap(filter = mcVersionFilter)

        val flatVersions = mcVersionsMap.flatMap { (versionIdentifier, numbers) ->
            numbers.map {  (buildIdentifier, fullversion) ->
                "$versionIdentifier/$buildIdentifier" to fullversion
            }
        }

        val allVersions = mcVersionsMap.flatMap { it.value.values }
        val promos = ForgeUtil.promoMap()
        val filteredPromos = promos.filterValues { version ->
            allVersions.contains(version)
        }

        return filteredPromos + flatVersions
    }

    suspend fun generateFabricIntermediariesAutocomplete(
        requireStable: Boolean = false,
        versionsFilter: List<String> = emptyList()
    ): Map<String, String> {
        return FabricUtil.getIntermediaries()
            .run {
                if(versionsFilter.isNotEmpty()) filter { it.version in versionsFilter } else this
            }
            .run {
                if(requireStable) filter { it.stable } else this
            }
            .associate {
                it.version to it.version
            }
            .toSortedMap()
    }

    suspend fun generateFabricLoadersAutocomplete(
        requireStable: Boolean = false
    ): Map<String, String> {
        return FabricUtil.getLoaders().filter {
            !requireStable || it.stable
        }.associate {
            it.version to it.version
        }.toSortedMap()
    }
    suspend fun generateFabricInstallersAutocomplete(
        requireStable: Boolean = false
    ): Map<String, String> {
        return FabricUtil.getInstallers().filter {
            !requireStable || it.stable
        }.associate {
            it.version to it.version
        }.toSortedMap()
    }

    fun generateCurseforgeKt(
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
            json.decodeFromString(MapSerializer(ProjectID, String.serializer()), curseSlugsFile.readText())
        } else {
            mapOf()
        } + slugIdMap.map { (slug, id) ->
            id to slug
        }
        curseSlugsFile.parentFile.mkdirs()
        curseSlugsFile.createNewFile()
        curseSlugsFile.writeText(json.encodeToString(MapSerializer(ProjectID, String.serializer()), allSlugIdMap))


        val targetFile = folder.resolve("$name.kt")
        if (targetFile.exists()) {
            logger.info("skipping generation of $targetFile")
            logger.info("file size: ${targetFile.length() / 1024.0} MB")
            return targetFile
        }

        val idType = ProjectID::class.asTypeName()
        val objectBuilder = TypeSpec.objectBuilder(name)
        objectBuilder.addKdoc("${section.sectionName} generated from mc versions: $gameVersions")
        slugIdMap.entries.sortedBy { (slug, id) ->
            slug
        }.forEach { (slug, id) ->
            val projectPage = when (section) {
                CurseSection.MODS -> "https://www.curseforge.com/minecraft/mc-mods/$slug"
                CurseSection.RESOURCE_PACKS -> "https://www.curseforge.com/minecraft/texture-packs/$slug"
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

    suspend fun generateForgeKt(
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

        val mcVersions = ForgeUtil.mcVersionsMapSanitized(filter = mcVersionFilters)
        val allVersions = mcVersions.flatMap { it.value.values }
        mcVersions.forEach { (versionIdentifier, numbers) ->
            val versionBuilder = TypeSpec.objectBuilder(versionIdentifier)
            for ((buildIdentifier, version) in numbers) {
                versionBuilder.addProperty(buildProperty(buildIdentifier, version))
            }
            forgeBuilder.addType(versionBuilder.build())
        }

        val promos = ForgeUtil.promoMapSanitized()
        for ((keyIdentifier, version) in promos) {
            if (allVersions.contains(version)) {
                forgeBuilder.addProperty(buildProperty(keyIdentifier, version))
            }
        }

        return save(forgeBuilder.build(), targetFile)
    }

    suspend fun generateFabricKt(
        name: String = "Fabric",
        mcVersionFilters: List<String>? = null,
        stable: Boolean = false,
        folder: File
    ): File {
        val targetFile = folder.resolve("$name.kt")
//        if (targetFile.exists()) {
//            logger.info("skipping generation of $targetFile")
//            logger.info("file size: ${targetFile.length() / 1024.0} MB")
//            return targetFile
//        }
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

        fun TypeSpec.Builder.addGetterProperty(type: ClassName, fieldName: String, value: String) {
            addProperty(
                PropertySpec.builder(
                    defaultSlugSanitizer(fieldName),
                    type
                )
                    .mutable(false)
                    .getter(
                        FunSpec.getterBuilder()
                            .addModifiers(KModifier.INLINE)
                            .addCode("return %T(%S)\n", type, value)
                            .build()
                    )
                    .build()
            )
        }

        val fabricBuilder = TypeSpec.objectBuilder(name)

        val intermediaries = FabricUtil.getIntermediaries().map {
            it.version
        }
        val intermediaryBuilder = TypeSpec.objectBuilder("intermediary")
        intermediaries.forEach { version ->
//            intermediaryBuilder.addProperty(buildProperty(version, version))
            intermediaryBuilder.addGetterProperty(
                type = IntermediaryVersion::class.asTypeName(),
                fieldName = "v_"+version,
                value = version
            )
        }
        fabricBuilder.addType(intermediaryBuilder.build())

        // TODO: use pairs from: https://meta.fabricmc.net/v2/versions/loader/1.15 for each gameVersion

        val loaders = FabricUtil.getLoaders().filter {
            !stable || it.stable
        }.map {
            it.version
        }
        val loadersBuilder = TypeSpec.objectBuilder("loader")
        loaders.forEach { version ->
//            loadersBuilder.addProperty(buildProperty(version, version))
            loadersBuilder.addGetterProperty(
                type = LoaderVersion::class.asTypeName(),
                fieldName =  "v_"+version,
                value = version
            )
        }
        fabricBuilder.addType(loadersBuilder.build())

        val installers = FabricUtil.getInstallers().filter {
            !stable || it.stable
        }.map {
            it.version
        }
        val installersBuilder = TypeSpec.objectBuilder("installer")
        installers.forEach { version ->
//            installersBuilder.addProperty(buildProperty(version, version))
            installersBuilder.addGetterProperty(
                type = InstallerVersion::class.asTypeName(),
                fieldName =  "v_"+version,
                value = version
            )
        }
        fabricBuilder.addType(installersBuilder.build())

        return save(fabricBuilder.build(), targetFile)
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

    suspend fun requestSlugIdMap(
        section: String,
        categories: List<String>? = null,
        gameVersions: List<String>? = null
    ): Map<String, ProjectID> =
        CurseClient.graphQLRequest(
            section = section,
            categories = categories,
            gameVersions = gameVersions
        ).map { (id, slug) ->
            slug to id
        }.toMap()
}