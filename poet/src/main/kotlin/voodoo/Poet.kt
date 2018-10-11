package voodoo

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
import voodoo.forge.ForgeUtil
import java.io.File

fun main(vararg args: String) {
    poet(rootDir = File(args[0]), root = File(args[1]))
}

fun poet(
    rootDir: File = File(System.getProperty("user.dir")).resolve(".voodoo"),
    root: File = rootDir.resolve(".voodoo"),
    mods: String = "Mod",
    texturePacks: String = "TexturePack",
    slugSanitizer: (String) -> String = Poet::defaultSlugSanitizer
) {
//    class XY
//    println("classloader is of type:" + Thread.currentThread().contextClassLoader)
//    println("classloader is of type:" + ClassLoader.getSystemClassLoader())
//    println("classloader is of type:" + XY::class.java.classLoader)
    Thread.currentThread().contextClassLoader = Poet::class.java.classLoader

    runBlocking {
        Poet.generate(
            name = mods,
            slugIdMap = Poet.requestMods(),
            slugSanitizer = slugSanitizer,
            folder = root
        )

        Poet.generate(
            name = texturePacks,
            slugIdMap = Poet.requestResourcePacks(),
            slugSanitizer = slugSanitizer,
            folder = root
        )

        Poet.generateForge("Forge", folder = root)

        Poet.generateConstants("Constants", rootDir, root)
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
    ) {
//        val curseModType = CurseMod::class.asTypeName()
        val objectBuilder = TypeSpec.objectBuilder(name)
        slugIdMap.entries.sortedBy { (slug, id) ->
            slug
        }.forEach { (slug, id) ->
            val projectPage = "https://minecraft.curseforge.com/projects/$slug"
            objectBuilder.addProperty(
                PropertySpec.builder(
                    slugSanitizer(slug),
                    Int::class,
                    KModifier.CONST
                )
                    .addKdoc("@see %L\n", projectPage)
                    .mutable(false)
                    .initializer("%L", id.value)
                    .build()
            )
        }

        save(objectBuilder.build(), name, folder)
    }

    internal suspend fun generateForge(
        name: String = "Forge",
        folder: File
    ) {
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

        for ((version, numbers) in forgeData.mcversion) {
            val versionIdentifier = "mc" + version.replace('.', '_')
            val versionBuilder = TypeSpec.objectBuilder(versionIdentifier)
            for (number in numbers) {
                val buildIdentifier = "build$number"
                versionBuilder.addProperty(buildProperty(buildIdentifier, number))
            }
            forgeBuilder.addType(versionBuilder.build())
        }

        for ((key, number) in forgeData.promos) {
            val keyIdentifier = key.replace('-', '_').replace('.', '_').run {
                if (this.first().isDigit())
                    "mc$this"
                else
                    this
            }
            forgeBuilder.addProperty(buildProperty(keyIdentifier, number))
        }

        save(forgeBuilder.build(), name, folder)
    }

    internal suspend fun generateConstants(
        name: String = "Constants",
        rootDir: File,
        folder: File
    ) {
        val constBuilder = TypeSpec.objectBuilder(name)

        val file = File::class.asClassName()
        val rootDirProperty = PropertySpec
            .builder("rootDir", file)
            .initializer("%T(%S)", file, rootDir.absoluteFile.path)
            .build()

        constBuilder.addProperty(rootDirProperty)

        save(constBuilder.build(), name, folder)
    }

    private fun save(type: TypeSpec, name: String, folder: File) {
        folder.mkdirs()
        val source = FileSpec.get("", type)
        val path = folder.apply {
            absoluteFile.parentFile.mkdirs()
        }.absoluteFile
        val targetFile = path.resolve("$name.kt")
        source.writeTo(path)
        logger.info("written to $targetFile")
    }

    suspend fun requestMods(): Map<String, ProjectID> =
        CurseClient.graphQLRequest("gameID: 432, section: MC_ADDONS").map { (id, slug) ->
            slug to ProjectID(id)
        }.toMap()

    suspend fun requestResourcePacks(): Map<String, ProjectID> =
        CurseClient.graphQLRequest("gameID: 432, section: TEXTURE_PACKS").map { (id, slug) ->
            slug to ProjectID(id)
        }.toMap()
}