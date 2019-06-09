package voodoo.poet

import com.skcraft.launcher.model.launcher.LaunchModifier
import com.skcraft.launcher.model.modpack.Feature
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.asClassName
import kotlinx.coroutines.runBlocking
import mu.KLogging
import voodoo.curse.CurseClient
import voodoo.data.curse.FileID
import voodoo.data.curse.ReleaseType
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.forge.ForgeUtil
import voodoo.provider.CurseProvider
import voodoo.provider.DirectProvider
import voodoo.provider.JenkinsProvider
import voodoo.provider.LocalProvider
import voodoo.provider.Providers
import voodoo.provider.UpdateJsonProvider
import voodoo.script.MainScriptEnv
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
            entry.enabled.takeIf { it != default.enabled }?.let {
                addStatement("enabled = %L", it)
            }
            when (provider) {
                is CurseProvider -> {
                    entry.curseReleaseTypes.takeIf { it != default.curseReleaseTypes }?.let { curseReleaseTypes ->
                        val fileType = ReleaseType::class.asClassName()
                        val builder = CodeBlock.builder().add("RELEASE_TYPES = setOf(")
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
                // categoryId changed
                entry.id != default.id -> when (provider) {
                    is CurseProvider -> {
                        val identifier = runBlocking {
                            val addon = CurseClient.getAddon(entry.curseProjectID)
                            val slug = addon!!.slug
                            Poet.defaultSlugSanitizer(slug)
                        }
                        if (entryBody.isEmpty())
                            addStatement("+Mod.$identifier")
                        else
                            beginControlFlow("+Mod.$identifier configure")
                    }
                    else -> {
                        if (entryBody.isEmpty())
                            addStatement("+%S", entry.id)
                        else
                            beginControlFlow("+%S configure", entry.id)
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
        val mainEnv = CodeBlock.builder().let { mainEnv ->
            val default = MainScriptEnv(rootDir = nestedPack.rootDir, id = nestedPack.id).apply {
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
            nestedPack.icon.takeIf { it != default.icon }?.let {
                mainEnv.addStatement("icon = rootDir.resolve(%S)", it.relativeTo(nestedPack.rootDir).path)
            }
            nestedPack.authors.takeIf { it != default.authors }?.let { authors ->
                mainEnv.addStatement("authors = listOf(%L)", authors.joinToString { """"$it"""" })
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
                        for ((identifier, version) in mapping) {
                            if (forge == version) return@runBlocking identifier
                        }
                    }
                    null
                }
                logger.info("forge literal guess: $forgeLiteral")

                if (forgeLiteral != null) {
                    mainEnv.addStatement("forge = Forge.%L", forgeLiteral)
                } else {
                    mainEnv.addStatement("forge = %L", forge)
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
            nestedPack.launch.takeIf { it != default.launch }?.let { launch ->
                mainEnv.addStatement(
                    """launch = %T(
                            |    flags = listOf(%L),
                            |)""".trimMargin(),
                    LaunchModifier::class.asClassName(),
                    launch.flags.joinToString { """"$it"""" }
                )
            }
            nestedPack.localDir.takeIf { it != default.localDir }?.let {
                mainEnv.addStatement("localDir = %S", it)
            }
            nestedPack.sourceDir.takeIf { it != default.sourceDir }?.let {
                mainEnv.addStatement("sourceDir = %S", it)
            }
            val rootEntry = nestedPack.root
            val provider = Providers[rootEntry.provider]
            mainEnv.controlFlow(
                "%T(%T)",
                ClassName("", "root"),
                provider::class.asClassName()
            ) { rootBuilder ->
                rootBuilder.entry(
                    rootEntry,
                    NestedEntry(rootEntry.provider),
                    true
                )
            }
        }

            .build()

        println("script: \n$mainEnv")

        val packScript = mainEnv.toString()
        folder.mkdirs()
        val scriptFile = folder.resolve("${nestedPack.id}.voodoo.kts")

        if (scriptFile.exists()) {
            logger.error { "file: $scriptFile already exists" }
            throw IllegalStateException("file: $scriptFile already exists")
        }
        scriptFile.writeText(packScript)

        logger.info { packScript }
        logger.info("written to $scriptFile")
    }
}