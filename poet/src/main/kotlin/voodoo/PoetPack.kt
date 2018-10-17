package voodoo

import com.skcraft.launcher.model.launcher.LaunchModifier
import com.skcraft.launcher.model.modpack.Feature
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.asClassName
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import voodoo.curse.CurseClient
import voodoo.data.UserFiles
import voodoo.data.curse.CurseConstants
import voodoo.data.curse.FileID
import voodoo.data.curse.FileType
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.forge.ForgeUtil
import voodoo.provider.CurseProvider
import voodoo.provider.DirectProvider
import voodoo.provider.JenkinsProvider
import voodoo.provider.LocalProvider
import voodoo.provider.Providers
import voodoo.provider.UpdateJsonProvider
import java.io.File

object PoetPack : KLogging() {
    fun CodeBlock.Builder.controlFlow(
        controlFlow: String,
        vararg args: Any?,
        buildFlow: (controlFlow: CodeBlock.Builder) -> Unit
    ): CodeBlock.Builder =
        beginControlFlow(controlFlow, *args)
            .apply(buildFlow)
            .endControlFlow()

    private fun CodeBlock.Builder.entry(entry: NestedEntry, default: NestedEntry, root: Boolean = false) {
        val provider = Providers[entry.provider]
        val entryBody = CodeBlock.builder().apply {
            entry.name.takeIf { it != default.name }?.let {
                addStatement("name = %S", it)
            }
            entry.folder.takeIf { it != default.folder }?.let {
                addStatement("folder = %S", it)
            }
            entry.comment.takeIf { it != default.comment }?.let {
                addStatement("comment = %S", it)
            }
            entry.description.takeIf { it != default.description }?.let {
                addStatement("description = %S", it)
            }
            entry.feature.takeIf { it != default.feature }?.let { feature ->
                val default = Feature()
                controlFlow("feature") { featureBuilder ->
                    feature.name.takeIf { it != default.name }?.let {
                        featureBuilder.addStatement("name = %S", it)
                    }
                    feature.selected.takeIf { it != default.selected }?.let {
                        featureBuilder.addStatement("selected = %L", it)
                    }
                    feature.recommendation.takeIf { it != default.recommendation }?.let {
                        featureBuilder.addStatement("recommendation = %L", it)
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
            entry.enabled.takeIf { it != default.enabled }?.let {
                addStatement("enabled = %L", it)
            }
            when (provider) {
                is CurseProvider -> {
                    entry.curseMetaUrl.takeIf { it != default.curseMetaUrl }?.let {
                        addStatement("curseMetaUrl = %S", it)
                    }
                    entry.curseReleaseTypes.takeIf { it != default.curseReleaseTypes }?.let { curseReleaseTypes ->
                        val fileType = FileType::class.asClassName()
                        val builder = CodeBlock.builder().add("releaseTypes = setOf(")
                        curseReleaseTypes.forEachIndexed { index, releaseType ->
                            if (index != 0) builder.add(", ")
                            builder.add("%T.%L", fileType, releaseType)
                        }
                        builder.add(")")
                        add("%[")
                        add(builder.build())
                        add("\n%]")
                    }
//                    entry.curseProjectID.takeIf { it != default.curseProjectID }?.let {
//                        addStatement("curseProjectID = %T(%L)", ProjectID::class.asClassName(), it.value)
//                    }
                    entry.curseFileID.takeIf { it != default.curseFileID }?.let {
                        addStatement("curseFileID = %T(%L)", FileID::class.asClassName(), it.value)
                    }
                }
                is DirectProvider -> {
                    entry.url.takeIf { it != default.url }?.let {
                        addStatement("url = %S", it)
                    }
                    entry.useUrlTxt.takeIf { it != default.useUrlTxt }?.let {
                        addStatement("useUrlTxt = %L", it)
                    }
                }
                is JenkinsProvider -> {
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
                is LocalProvider -> {
                    entry.fileSrc.takeIf { it != default.fileSrc }?.let {
                        addStatement("fileSrc = %S", it)
                    }
                }
                is UpdateJsonProvider -> {
                    entry.updateJson.takeIf { it != default.updateJson }?.let {
                        addStatement("updateJson = %S", it)
                    }
                    entry.updateChannel.takeIf { it != default.updateChannel }?.let {
                        addStatement("updateChannel = %L", it)
                    }
                    entry.template.takeIf { it != default.template }?.let {
                        addStatement("template = %S", it)
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
                entry.id != default.id -> when (provider) {
                    is CurseProvider -> {
                        val identifier = runBlocking {
                            val addon = CurseClient.getAddon(entry.curseProjectID, CurseConstants.PROXY_URL)
                            val slug = addon!!.slug
                            Poet.defaultSlugSanitizer(slug)
                        }
                        if (entryBody.isEmpty())
                            addStatement("+ Mod::$identifier")
                        else
                            beginControlFlow("%T(Mod.$identifier)", ClassName("", "add"))
                    }
                    else -> {
                        if (entryBody.isEmpty())
                            addStatement("+%S", entry.id)
                        else
                            beginControlFlow("%T(%S)", ClassName("", "add"), entry.id)
                    }
                }
            // provider changed
                entry.provider != default.provider -> if (entryBody.isEmpty()) addStatement(
                    "%T(%T)",
                    ClassName("", "withProvider"),
                    provider::class.asClassName()
                ) else beginControlFlow(
                    "%T(%T)",
                    ClassName("", "withProvider"),
                    provider::class.asClassName()
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
                    listBuilder.entry(subEntry, NestedEntry(entry.provider))
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
        val mainFunCall = CodeBlock.builder()
            .controlFlow(
                """return %T(
                |    root = Constants.rootDir,
                |    arguments = args
                |)""".trimMargin(),
                ClassName("voodoo", "withDefaultMain")
            ) { mainEnv ->
                mainEnv.controlFlow(
                    """nestedPack(
                    |    id = %S,
                    |    mcVersion = %S
                    |)""".trimMargin(),
                    nestedPack.id,
                    nestedPack.mcVersion
                ) { nestedBuilder ->
                    val default = NestedPack(
                        rootDir = nestedPack.rootDir,
                        id = nestedPack.id,
                        mcVersion = nestedPack.mcVersion
                    )
                    nestedPack.title.takeIf { it != default.title }?.let {
                        nestedBuilder.addStatement("title = %S", it)
                    }
                    nestedPack.version.takeIf { it != default.version }?.let {
                        nestedBuilder.addStatement("version = %S", it)
                    }
                    nestedPack.icon.takeIf { it != default.icon }?.let {
                        nestedBuilder.addStatement("icon = rootDir.resolve(%S)", it.relativeTo(nestedPack.rootDir).path)
                    }
                    nestedPack.authors.takeIf { it != default.authors }?.let { authors ->
                        nestedBuilder.addStatement("authors = listOf(%L)", authors.joinToString { """"$it"""" })
                    }
                    nestedPack.forge.takeIf { it != default.forge }?.let { forge ->
                        logger.info("guessing forge literal for $forge")
                        val forgeLiteral = runBlocking {
                            val promos = ForgeUtil.promoMap()
                            for ((identifier, number) in promos) {
                                if (forge == number) return@runBlocking identifier
                            }
                            val mcVersions = ForgeUtil.mcVersionsMap()
                            for ((_, mapping) in mcVersions) {
                                for ((identifier, number) in mapping) {
                                    if (forge == number) return@runBlocking identifier
                                }
                            }
                            null
                        }
                        logger.info("forge literal guess: $forgeLiteral")

                        if (forgeLiteral != null) {
                            nestedBuilder.addStatement("forge = Forge.%L", forgeLiteral)
                        } else {
                            nestedBuilder.addStatement("forge = %L", forge)
                        }
                    }
                    nestedPack.userFiles.takeIf { it != default.userFiles }?.let { userFiles ->
                        nestedBuilder.addStatement(
                            """userFiles = %T(
                            |    include = listOf(%L),
                            |    exclude = listOf(%L)
                            |)""".trimMargin(),
                            UserFiles::class.asClassName(),
                            userFiles.include.joinToString { """"$it"""" },
                            userFiles.exclude.joinToString { """"$it"""" }
                        )
                    }
                    nestedPack.launch.takeIf { it != default.launch }?.let { launch ->
                        nestedBuilder.addStatement(
                            """launch = %T(
                            |    flags = listOf(%L),
                            |)""".trimMargin(),
                            LaunchModifier::class.asClassName(),
                            launch.flags.joinToString { """"$it"""" }
                        )
                    }
                    nestedPack.localDir.takeIf { it != default.localDir }?.let {
                        nestedBuilder.addStatement("localDir = %S", it)
                    }
                    nestedPack.sourceDir.takeIf { it != default.sourceDir }?.let {
                        nestedBuilder.addStatement("sourceDir = %S", it)
                    }
                    val rootEntry = nestedPack.root
                    val provider = Providers[rootEntry.provider]
                    nestedBuilder.controlFlow(
                        "root = %T(%T)",
                        ClassName("", "rootEntry"),
                        provider::class.asClassName()
                    ) { rootBuilder ->
                        rootBuilder.entry(
                            rootEntry,
                            NestedEntry(rootEntry.provider),
                            true
                        )
                    }
                }
            }
            .build()
        val mainFun = FunSpec.builder("main")
            .addParameter("args", String::class, KModifier.VARARG)
            .addCode(mainFunCall)
            .build()
        val fileSpec = FileSpec.builder("", nestedPack.id)
            .addFunction(mainFun)
            .build()
        folder.mkdirs()
        fileSpec.writeTo(folder)
        logger.info { folder }
        logger.info { fileSpec }
    }
}