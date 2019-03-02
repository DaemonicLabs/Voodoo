package voodoo.script

import voodoo.GenerateForge
import voodoo.GenerateMods
import voodoo.GenerateTexturePacks
import voodoo.Include
import voodoo.data.curse.Section
import voodoo.poet.Poet
import voodoo.poet.generator.CurseGenerator
import voodoo.poet.generator.ForgeGenerator
import voodoo.util.SharedFolders
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCollectedData
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.foundAnnotations
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.api.importScripts
import kotlin.script.experimental.api.refineConfiguration
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.toScriptSource

object MainScriptEnvConfiguration : ScriptCompilationConfiguration({
    defaultImports.append(
        "voodoo.*",
        "voodoo.dsl.*",
        "voodoo.provider.*",
        "voodoo.data.*",
        "voodoo.data.curse.*",
        "voodoo.provider.*",
        "com.skcraft.launcher.model.modpack.Recommendation"
    )
    compilerOptions.append("-jvm-target 1.8")

    refineConfiguration {
//        onAnnotations<Include>(Include.Companion::configureIncludes)

//        beforeParsing { context ->
//            val reports: MutableList<ScriptDiagnostic> = mutableListOf()
//            val annotations = context.collectedData?.get(ScriptCollectedData.foundAnnotations).also {
//                reports += ScriptDiagnostic("found annotations: '$it'", severity = ScriptDiagnostic.Severity.INFO)
//            }
//            println("bp annotations: $annotations")
//            context.compilationConfiguration.asSuccess(reports)
//        }

        onAnnotations(listOf(GenerateMods::class, GenerateTexturePacks::class, GenerateForge::class, Include::class)) { context ->
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
            // TODO? make sure rootFolder points at the correct folder

            require(context.script is FileScriptSource) { "${context.script::class} != FileScriptSource" }
            val scriptFile = (context.script as FileScriptSource).file
            SharedFolders.RootDir.default = scriptFile.parentFile.parentFile
            val rootDir = SharedFolders.RootDir.get()

            val id = scriptFile.name.substringBeforeLast(".voodoo.kts").toLowerCase()

            val generatedFilesDir = SharedFolders.GeneratedSrc.get(id).absoluteFile
            reports += ScriptDiagnostic("generatedFilesDir: $generatedFilesDir", ScriptDiagnostic.Severity.DEBUG)

            // collect generator instructions
            val modGenerators = annotations.filter { it is GenerateMods }.map { annotation ->
                annotation as GenerateMods
            }.groupBy { it.name }

            val texturePackGenerators = annotations.filter { it is GenerateTexturePacks }.map { annotation ->
                annotation as GenerateTexturePacks
            }.groupBy { it.name }

            val curseGenerators = modGenerators.map { (file, annotations) ->
                CurseGenerator(
                    name = file,
                    section = Section.MODS,
                    mcVersions = annotations.map { it.mc }.filter { it.isNotBlank() }.distinct()
                )
            } + texturePackGenerators.map { (file, annotations) ->
                CurseGenerator(
                    name = file,
                    section = Section.TEXTURE_PACKS,
                    mcVersions = annotations.map { it.mc }.filter { it.isNotBlank() }.distinct()
                )
            }
            reports += ScriptDiagnostic("curseGenerators: $curseGenerators", ScriptDiagnostic.Severity.DEBUG)

            val forgeGenerators = annotations.filter { it is GenerateForge }.map { annotation ->
                annotation as GenerateForge
            }.groupBy { it.name }
                .map { (file, annotations) ->
                    ForgeGenerator(
                        name = file,
                        mcVersions = annotations.map { it.mc }.filter { it.isNotBlank() }.distinct()
                    )
                }

            reports += ScriptDiagnostic("forgeGenerators: $forgeGenerators", ScriptDiagnostic.Severity.DEBUG)

            val generatedFiles = Poet.generateAll(
                curseGenerators = curseGenerators,
                forgeGenerators = forgeGenerators,
                generatedSrcDir = generatedFilesDir
            )

            val includeCompilationConfiguration = Include.configureIncludes(reports, context)

            val compilationConfiguration = ScriptCompilationConfiguration(includeCompilationConfiguration) {
                ide.acceptedLocations.append(ScriptAcceptedLocation.Project)
                importScripts.append(generatedFiles.map { it.toScriptSource() })
                reports += ScriptDiagnostic(
                    "generated: ${generatedFiles.map { it.relativeTo(rootDir) }}",
                    ScriptDiagnostic.Severity.INFO
                )
            }

//            compilationConfiguration.entries().forEach {
//                println("beforeParsing    $it")
//            }

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
