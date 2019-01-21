package voodoo

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import kotlinx.coroutines.runBlocking
import mu.KLogging
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
import voodoo.forge.ForgeUtil
import java.io.File

fun main(vararg args: String) {
    poet(rootDir = File(args[0]), generatedSrcDir = File(args[1]))
}

fun poet(
    rootDir: File = File(System.getProperty("user.dir")),
    generatedSrcDir: File = rootDir.resolve(".voodoo"),
    mods: String = "Mod",
    texturePacks: String = "TexturePack",
    slugSanitizer: (String) -> String = Poet::defaultSlugSanitizer
): List<File> {
//    class XY
//    println("classloader is of type:" + Thread.currentThread().contextClassLoader)
//    println("classloader is of type:" + ClassLoader.getSystemClassLoader())
//    println("classloader is of type:" + XY::class.java.classLoader)
    Thread.currentThread().contextClassLoader = Poet::class.java.classLoader

    return runBlocking {
        listOf(
            Poet.generate(
                name = mods,
                slugIdMap = Poet.requestMods(),
                slugSanitizer = slugSanitizer,
                folder = generatedSrcDir
            ),
            Poet.generate(
                name = texturePacks,
                slugIdMap = Poet.requestResourcePacks(),
                slugSanitizer = slugSanitizer,
                folder = generatedSrcDir
            ),
            Poet.generateForge("Forge", folder = generatedSrcDir)
        )
    }
}

object Poet : KLogging() {
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
    ) : File {
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

        val mcVersions = ForgeUtil.mcVersionsMap()
        for ((versionIdentifier, numbers) in mcVersions) {
            val versionBuilder = TypeSpec.objectBuilder(versionIdentifier)
            for ((buildIdentifier, number) in numbers) {
                versionBuilder.addProperty(buildProperty(buildIdentifier, number))
            }
            forgeBuilder.addType(versionBuilder.build())
        }

        val promos = ForgeUtil.promoMap()
        for ((keyIdentifier, number) in promos) {
            forgeBuilder.addProperty(buildProperty(keyIdentifier, number))
        }

        return save(forgeBuilder.build(), name, folder)
    }

    private fun save(source: FileSpec, name: String, folder: File) : File  {
        val path = folder.apply {
            absoluteFile.parentFile.mkdirs()
        }.absoluteFile
        val targetFile = path.resolve("$name.kt")
        source.writeTo(path)
        logger.info("written to $targetFile")
        return targetFile
    }

    private fun save(type: TypeSpec, name: String, folder: File) : File {
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

    suspend fun requestMods(): Map<String, ProjectID> =
        CurseClient.graphQLRequest("section: MC_ADDONS").map { (id, slug) ->
            slug to ProjectID(id)
        }.toMap()

    suspend fun requestResourcePacks(): Map<String, ProjectID> =
        CurseClient.graphQLRequest("section: TEXTURE_PACKS").map { (id, slug) ->
            slug to ProjectID(id)
        }.toMap()
}