package voodoo.script

import mu.KotlinLogging
import voodoo.GenerateForge
import voodoo.GenerateMods
import voodoo.GenerateResourcePacks
import voodoo.poet.Poet
import voodoo.poet.generator.CurseGenerator
import voodoo.poet.generator.CurseSection
import voodoo.poet.generator.ForgeGenerator
import voodoo.script.annotation.Include
import voodoo.util.SharedFolders
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.toScriptSource

object MainScriptEnvConfiguration : ScriptCompilationConfiguration({
    val logger = KotlinLogging.logger {}

    val imports = listOf(
        voodoo.data.UserFiles::class,
        voodoo.data.DependencyType::class,
        voodoo.data.PackOptions::class,
        voodoo.data.curse.FileType::class,
        voodoo.data.curse.ProjectID::class,
        voodoo.data.curse.FileID::class,

//        voodoo.data.nested.NestedEntry::class,
        voodoo.data.nested.NestedEntry.Common::class,
        voodoo.data.nested.NestedEntry.Curse::class,
        voodoo.data.nested.NestedEntry.Direct::class,
        voodoo.data.nested.NestedEntry.Jenkins::class,
        voodoo.data.nested.NestedEntry.Local::class,
        voodoo.data.nested.NestedEntry.Noop::class,

        moe.nikky.voodoo.format.FnPatternList::class,

        moe.nikky.voodoo.format.modpack.Recommendation::class,
        moe.nikky.voodoo.format.Feature::class,

        GenerateForge::class,
        GenerateMods::class,
        GenerateResourcePacks::class,
        Include::class
    ).map {
        it.qualifiedName!!
    }

    defaultImports.append(
        "voodoo.*",
        "voodoo.script.annotation.Include",
        "voodoo.dsl.*",
        "voodoo.provider.CurseProvider",
        "voodoo.provider.DirectProvider",
        "voodoo.provider.JenkinsProvider",
        "voodoo.provider.LocalProvider",
        "voodoo.provider.UpdateJsonProvider",
        "voodoo.data.*",
        "voodoo.data.curse.*",
        "voodoo.data.nested.NestedEntry.*",
        "moe.nikky.voodoo.format.*",
        "com.skcraft.launcher.model.SKServer",
        "com.skcraft.launcher.model.modpack.Recommendation",
        *(imports.toTypedArray())
    )

    compilerOptions.append("-jvm-target", "1.8")

    refineConfiguration {
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }

//        onAnnotations<Include>(Include.Companion::configureIncludes)

//        importScripts.append()

        beforeParsing { context ->
            val reports: MutableList<ScriptDiagnostic> = mutableListOf()

            require(context.script is FileBasedScriptSource) { "${context.script::class} != FileBasedScriptSource" }
            val scriptFile = (context.script as FileBasedScriptSource).file
            // TODO? make sure rootFolder points at the correct folder
            SharedFolders.RootDir.value = scriptFile.parentFile.parentFile
            val rootDir = SharedFolders.RootDir.get()

            val generatedSharedSrc = SharedFolders.GeneratedSrcShared.get()

            val globalGeneratedFiles = generatedSharedSrc.listFiles { file -> file.extension == "kt" }!!.toList()

//            val reports: MutableList<ScriptDiagnostic> = mutableListOf()
//            val annotations = context.collectedData?.get(ScriptCollectedData.foundAnnotations).also {
//                reports += ScriptDiagnostic("found annotations: '$it'", severity = ScriptDiagnostic.Severity.INFO)
//            }
//            println("bp annotations: $annotations")

            val compilationConfiguration = ScriptCompilationConfiguration(context.compilationConfiguration) {
                importScripts.append(globalGeneratedFiles.map { it.toScriptSource() })
                reports += ScriptDiagnostic(
                    "adding global generated files: ${globalGeneratedFiles.map { it.relativeTo(rootDir) }}",
                    ScriptDiagnostic.Severity.INFO
                )
            }

            compilationConfiguration.asSuccess(reports)
        }

        onAnnotations(listOf(GenerateMods::class, GenerateResourcePacks::class, GenerateForge::class, Include::class)) { context ->
            val reports: MutableList<ScriptDiagnostic> = mutableListOf()
//            println("collectedData: '${context.collectedData}'")
//            context.collectedData?.entries()?.forEach { (key, value) ->
//                println("collectedData    $key => $value")
//            }
//            context.compilationConfiguration.entries().forEach { (key, value) ->
//                println("compilationConfiguration    $key => $value")
//            }
            val annotations = context.collectedData?.get(ScriptCollectedData.foundAnnotations).also { annotations ->
                annotations?.forEach { annotation ->
                    require(annotation::class.simpleName != "InvalidScriptResolverAnnotation") {
                        "InvalidScriptResolverAnnotation found"
                    }
                }
                reports += ScriptDiagnostic("found annotations: '$annotations'", severity = ScriptDiagnostic.Severity.DEBUG)
            }?.takeIf { it.isNotEmpty() }
                ?: run {
                    // TODO use a fallback generator ?
                    return@onAnnotations ScriptCompilationConfiguration(context.compilationConfiguration).asSuccess()
                }
//            org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
//            kotlin.script.experimental.host.FileBasedScriptSource
            require(context.script is FileBasedScriptSource) { "${context.script::class} != FileBasedScriptSource" }
            val scriptFile = (context.script as FileBasedScriptSource).file
            // TODO? make sure rootFolder points at the correct folder
            SharedFolders.RootDir.value = scriptFile.parentFile.parentFile
            val rootDir = SharedFolders.RootDir.get()

            val id = scriptFile.name.substringBeforeLast(".voodoo.kts").toLowerCase()

            val generatedFilesDir = SharedFolders.GeneratedSrc.get(id).absoluteFile
            reports += ScriptDiagnostic("generatedFilesDir: $generatedFilesDir", ScriptDiagnostic.Severity.DEBUG)

            // collect generator instructions
            val modGenerators = annotations.filterIsInstance<GenerateMods>().groupBy { it.name }

            val resourcePackGenerators = annotations.filterIsInstance<GenerateResourcePacks>().groupBy { it.name }

            val curseGenerators = modGenerators.map { (file, annotations) ->
                CurseGenerator(
                    name = file,
                    section = CurseSection.MODS,
                    mcVersions = annotations.map { it.mc }.filter { it.isNotBlank() }.distinct()
                )
            } + resourcePackGenerators.map { (file, annotations) ->
                CurseGenerator(
                    name = file,
                    section = CurseSection.RESOURCE_PACKS,
                    mcVersions = annotations.map { it.mc }.filter { it.isNotBlank() }.distinct()
                )
            }
            reports += ScriptDiagnostic("curseGenerators: $curseGenerators", ScriptDiagnostic.Severity.DEBUG)

            val forgeGenerators = annotations.filterIsInstance<GenerateForge>().groupBy { it.name }
                .map { (file, annotations) ->
                    ForgeGenerator(
                        name = file,
                        mcVersions = annotations.map { it.mc }.filter { it.isNotBlank() }.distinct()
                    )
                }

            reports += ScriptDiagnostic("forgeGenerators: $forgeGenerators", ScriptDiagnostic.Severity.DEBUG)

            // TODO: get generator from plugin

            val generatedFiles = Poet.generateAll(
                curseGenerators = curseGenerators,
                forgeGenerators = forgeGenerators,
                generatedSrcDir = generatedFilesDir
            )

            val includeCompilationConfiguration = Include.configureIncludes(reports, context)

            val compilationConfiguration = ScriptCompilationConfiguration(includeCompilationConfiguration) {
                importScripts.append(generatedFiles.map { it.toScriptSource() })
                reports += ScriptDiagnostic(
                    "generated: ${generatedFiles.map { it.relativeTo(rootDir) }}",
                    ScriptDiagnostic.Severity.INFO
                )
            }

//            compilationConfiguration.entries().forEach {
//                println("beforeParsing    $it")
//            }

//            exitProcess(-4)
            reports.forEach {
                System.err.println("${it.severity}: ${it.message}")
                logger.info { "${it.severity}: ${it.message}" }
            }
            compilationConfiguration.asSuccess(reports)
        }

//        beforeCompiling { context ->
//            val reports: MutableList<ScriptDiagnostic> = mutableListOf()
//            val annotations = context.collectedData?.get(ScriptCollectedData.foundAnnotations).also {
//                //                reports += ScriptDiagnostic("found annotations: '$it'", severity = ScriptDiagnostic.Severity.INFO)
//            }
//            println("bc annotations: $annotations")
//            annotations?.forEach {
//                println(it::class)
//                println(it)
//            }
//            context.compilationConfiguration.asSuccess(reports)
//        }
    }
})
