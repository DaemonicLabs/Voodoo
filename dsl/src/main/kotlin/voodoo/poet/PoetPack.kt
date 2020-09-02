package voodoo.poet

import com.squareup.kotlinpoet.*
import kotlinx.coroutines.runBlocking
import moe.nikky.voodoo.format.Feature
import mu.KLogging
import voodoo.GenerateForge
import voodoo.GenerateMods
import voodoo.GenerateResourcePacks
import voodoo.curse.CurseClient
import voodoo.data.ModloaderPattern
import voodoo.data.curse.FileID
import voodoo.data.curse.FileType
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.dsl.builder.ModpackBuilder
import voodoo.forge.ForgeUtil
import voodoo.provider.Providers
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.reflect.full.createInstance

object PoetPack : KLogging() {
    fun CodeBlock.Builder.controlFlow(
        controlFlow: String,
        vararg args: Any?,
        buildFlow: (controlFlow: CodeBlock.Builder) -> Unit
    ): CodeBlock.Builder =
        beginControlFlow(controlFlow, *args)
            .apply(buildFlow)
            .endControlFlow()

    private fun CodeBlock.Builder.entry(entry: NestedEntry, /*default: NestedEntry,*/ root: Boolean = false) {
        val default = entry::class.createInstance()
        val provider = Providers[entry.provider]
        val entryBody = CodeBlock.builder().apply {
            entry.name.takeIf { it != default.name }?.let {
                addStatement("name = %S", it)
            }
            entry.folder.takeIf { it != default.folder }?.let {
                addStatement("folder = %S", it)
            }
            entry.description.takeIf { it != default.description }?.let {
                addStatement("description = %S", it)
            }
            entry.optionalData.takeIf { it != default.optionalData }?.let { feature ->
                val defaultFeature = Feature()
                controlFlow("optional") { featureBuilder ->
                    feature.selected.takeIf { it != defaultFeature.selected }?.let {
                        featureBuilder.addStatement("selected = %L", it)
                    }
                    feature.skRecommendation.takeIf { it != defaultFeature.recommendation }?.let {
                        featureBuilder.addStatement("skRecommendation = %L", it)
                    }
                }
            }
            entry.side.takeIf { it != default.side }?.let {
                addStatement("side = %L", it)
            }
            entry.packageType.takeIf { it != default.packageType }?.let {
                addStatement("packageType = %L", it)
            }
            entry.transient.takeIf { it != default.transient }?.let {
                addStatement("transient = %L", it)
            }
            entry.version.takeIf { it != default.version }?.let {
                addStatement("version = %S", it)
            }
            entry.fileName.takeIf { it != default.fileName }?.let {
                addStatement("fileName = %S", it)
            }
            entry.fileNameRegex.takeIf { it != default.fileNameRegex }?.let {
                addStatement("fileNameRegex = %S", it)
            }
            entry.validMcVersions.takeIf { it != default.validMcVersions }?.let { validMcVersions ->
                addStatement("validMcVersions = setOf(%L)", validMcVersions.joinToString { """"$it"""" })
            }
            entry.invalidMcVersions.takeIf { it != default.invalidMcVersions }?.let { invalidMcVersions ->
                addStatement("invalidMcVersions = setOf(%L)", invalidMcVersions.joinToString { """"$it"""" })
            }
            entry.enabled.takeIf { it != default.enabled }?.let {
                addStatement("enabled = %L", it)
            }
            when (entry) {
                is NestedEntry.Curse -> {
                    default as NestedEntry.Curse
                    entry.releaseTypes.takeIf { it != default.releaseTypes }?.let { curseReleaseTypes ->
                        val fileType = FileType::class.asClassName()
                        val builder = CodeBlock.builder().add("releaseTypes = setOf(")
                        curseReleaseTypes.forEachIndexed { index, releaseType ->
                            if (index != 0) builder.add(", ")
                            builder.add("%T.%L", fileType, releaseType)
                        }
                        builder.add(")")
                        add("«")
                        add(builder.build())
                        add("\n»")
                    }
//                    entry.curseProjectID.takeIf { it != default.curseProjectID }?.let {
//                        addStatement("curseProjectID = %T(%L)", ProjectID::class.asClassName(), it.value)
//                    }
                    entry.fileID.takeIf { it != default.fileID }?.let {
                        addStatement("fileID = %T(%L)", FileID::class.asClassName(), it.value)
                    }
                }
                is NestedEntry.Direct -> {
                    default as NestedEntry.Direct
                    entry.url.takeIf { it != default.url }?.let {
                        addStatement("url = %S", it)
                    }
                    entry.useUrlTxt.takeIf { it != default.useUrlTxt }?.let {
                        addStatement("useUrlTxt = %L", it)
                    }
                }
                is NestedEntry.Jenkins -> {
                    default as NestedEntry.Jenkins
                    entry.jenkinsUrl.takeIf { it != default.jenkinsUrl }?.let {
                        addStatement("jenkinsUrl = %S", it)
                    }
                    entry.job.takeIf { it != default.job }?.let {
                        addStatement("job = %S", it)
                    }
                    entry.buildNumber.takeIf { it != default.buildNumber }?.let {
                        addStatement("buildNumber = %L", it)
                    }
                }
                is NestedEntry.Local -> {
                    default as NestedEntry.Local
                    entry.fileSrc.takeIf { it != default.fileSrc }?.let {
                        addStatement("fileSrc = %S", it)
                    }
                }
                else -> {
                    logger.info("unknown provider: ${provider::javaClass}")
                }
            }
        }.build()

        val builder = if (!root) {
            when {
                // id changed
                entry.id != default.id -> when (entry) {
                    is NestedEntry.Curse -> {
                        val identifier = runBlocking {
                            val addon = CurseClient.getAddon(entry.projectID)
                            val slug = addon!!.slug
                            Poet.defaultSlugSanitizer(slug)
                        }
                        if (entryBody.isEmpty())
                            addStatement("+Mod.$identifier")
                        else
                            beginControlFlow("+Mod.$identifier ")
                    }
                    else -> {
                        if (entryBody.isEmpty())
                            addStatement("+%S", entry.id)
                        else
                            beginControlFlow("+%S ", entry.id)
                    }
                }
                // provider changed
                entry::class.qualifiedName != default::class.qualifiedName -> if (entryBody.isEmpty()) addStatement(
                    "%T(%L::class)",
                    ClassName("", "withTypeClass"),
                    entry::class.simpleName
                ) else beginControlFlow(
                    "%T(%L::class)",
                    ClassName("", "withTypeClass"),
                    entry::class.simpleName
                )
                // everything else
                else -> beginControlFlow("%T", ClassName("", "group"))
            }
        } else null

        val indented = builder != null

        add(entryBody)

        if (indented && entryBody.isNotEmpty()) {
            endControlFlow()
        }

        entry.entries.takeUnless { it.isEmpty() }?.let { entries ->
            controlFlow("%T", ClassName("", if (root) "list" else ".list")) { listBuilder ->
                entries.sortedBy { it.id.toLowerCase() }.forEach { subEntry ->
                    listBuilder.entry(subEntry/*, NestedEntry(entry.provider)*/)
                }
            }
        }

//        if (!indented || !entryBody.isNotEmpty()) {
//            endControlFlow()
//        }
    }

    fun createModpack(
        folder: File,
        nestedPack: NestedPack
    ) {
        Thread.currentThread().contextClassLoader = PoetPack::class.java.classLoader
        val mainEnv = CodeBlock.builder().let { mainEnv ->
            val default = ModpackBuilder(NestedPack.create(rootFolder = nestedPack.rootFolder, id = nestedPack.id)).apply {
                mcVersion = nestedPack.mcVersion
            }
            nestedPack.mcVersion.let {
                mainEnv.addStatement("mcVersion = %S", it)
            }
            nestedPack.title.takeIf { it != default.title }?.let {
                mainEnv.addStatement("title = %S", it)
            }
            nestedPack.version.takeIf { it != default.version }?.let {
                mainEnv.addStatement("version = %S", it)
            }
            nestedPack.iconPath.takeIf { it != default.iconPath }?.let {
                mainEnv.addStatement("iconPath = %S", it)
            }
            nestedPack.authors.takeIf { it != default.authors }?.let { authors ->
                mainEnv.addStatement("authors = listOf(%L)", authors.joinToString { """"$it"""" })
            }
            nestedPack.modloader.takeIf { it != default.pack.modloader }?.let {
                logger.info { "generating modloader" }
                when (it) {
                    is ModloaderPattern.Forge -> {
                        logger.info("guessing forge literal for ${it.version}")
                        val forgeLiteral = runBlocking {
                            val promos = ForgeUtil.promoMap()
                            for ((identifier, number) in promos) {
                                if (it.version == number) return@runBlocking identifier
                            }
                            val mcVersions = ForgeUtil.mcVersionsMap()
                            for ((_, mapping) in mcVersions) {
                                for ((identifier, version) in mapping) {
                                    if (it.version == version) return@runBlocking identifier
                                }
                            }
                            null
                        }
                        logger.info("forge literal guess: $forgeLiteral")

                        if (forgeLiteral != null) {
                            mainEnv.addStatement("modloader { forge(Forge.%L) }", forgeLiteral)
                        } else {
                            mainEnv.addStatement("modloader { forge(%L) }", it.version)
                        }
                    }
                    is ModloaderPattern.Fabric -> {
                        // TODO: needs work ?
                        mainEnv.addStatement(
                            """
                            |modloader {
                            |    fabric(
                            |        intermediary = %L,
                            |        loader = %L,
                            |        installer = %L,
                            |    )
                            |}
                        """.trimMargin(),
                            it.intermediateMappingsVersion,
                            it.loaderVersion,
                            it.installerVersion
                        )
                    }
                    is ModloaderPattern.None -> {

                    }
                }
            }
            // TODO: readd along with other PackOptions
//            nestedPack.userFiles.takeIf { it != default.userFiles }?.let { userFiles ->
//                mainEnv.addStatement(
//                    """userFiles = %T(
//                            |    include = listOf(%L),
//                            |    exclude = listOf(%L)
//                            |)""".trimMargin(),
//                    UserFiles::class.asClassName(),
//                    userFiles.include.joinToString { """"$it"""" },
//                    userFiles.exclude.joinToString { """"$it"""" }
//                )
//            }
            nestedPack.localDir.takeIf { it != default.localDir }?.let {
                mainEnv.addStatement("localDir = %S", it)
            }
            val rootEntry = nestedPack.root
            mainEnv.controlFlow(
                "%T<%L>",
                ClassName("", "root"),
                rootEntry::class.simpleName
            ) { rootBuilder ->
                rootBuilder.entry(
                    rootEntry,
                    /* NestedEntry(rootEntry.provider),*/
                    true
                )
            }
        }

            .build()

        println("script: \n$mainEnv")

        val fileSpec = FileSpec.builder("", "${nestedPack.id}.voodoo.kts").also { fileSpecBuilder ->
            fileSpecBuilder.annotations += AnnotationSpec.builder(GenerateMods::class).also { annotationBuilder ->
                annotationBuilder.useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                annotationBuilder.addMember("name = %S", "Mod")
                annotationBuilder.addMember("mc = %S", nestedPack.mcVersion ?: "1.12.2")
            }.build()
            fileSpecBuilder.annotations += AnnotationSpec.builder(GenerateResourcePacks::class)
                .also { annotationBuilder ->
                    annotationBuilder.useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                    annotationBuilder.addMember("name = %S", "TexturePack")
                    annotationBuilder.addMember("mc = %S", nestedPack.mcVersion ?: "1.12.2")
                }.build()
            fileSpecBuilder.annotations += AnnotationSpec.builder(GenerateForge::class).also { annotationBuilder ->
                annotationBuilder.useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                annotationBuilder.addMember("name = %S", "Forge")
                annotationBuilder.addMember("mc = %S", nestedPack.mcVersion ?: "1.12.2")
            }.build()
        }.build()


        folder.mkdirs()
        val scriptFile = folder.resolve("${nestedPack.id}.voodoo.kts")

        if (scriptFile.exists()) {
            logger.error { "file: $scriptFile already exists" }
            throw IllegalStateException("file: $scriptFile already exists")
        }

        val outputPath = scriptFile.toPath()
        OutputStreamWriter(Files.newOutputStream(outputPath), StandardCharsets.UTF_8).use { writer ->
            fileSpec.writeTo(
                writer
            )
        }
        val packScript = mainEnv.toString()
        scriptFile.appendText("\n" + packScript)

        logger.info("written to $scriptFile")
    }
}