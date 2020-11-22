package voodoo.config

import com.github.ricky12awesome.jss.encodeToSchema
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import mu.KotlinLogging
import voodoo.poet.Poet
import voodoo.util.SharedFolders
import voodoo.util.json
import voodoo.util.toRelativeUnixPath
import java.io.File

object Autocompletions {
    private val logger = KotlinLogging.logger {}

    //    private val rootDir = SharedFolders.RootDir.get()
    val rootDir by lazy {
        SharedFolders.RootDir.get().absoluteFile
    }
    private val configFile by lazy {
        rootDir.resolve("config.json")
    }
    private val cacheDir by lazy {
        rootDir.resolve(".completions").apply { mkdirs() }
    }
    private val serializer = MapSerializer(String.serializer(), String.serializer())

    val curseforgeFile: File = cacheDir.resolve("curseforge.json")
    val forgeFile: File = cacheDir.resolve("forge.json")
    val fabricIntermediariesFile: File = cacheDir.resolve("fabric_intermediaries.json")
    val fabricInstallersFile: File = cacheDir.resolve("fabric_installers.json")
    val fabricLoadersFile: File = cacheDir.resolve("fabric_loaders.json")

    private val config by lazy {
        if (configFile.exists()) {
            json.decodeFromString(
                Configuration.serializer(),
                configFile.readText()
            )
        } else {
            Configuration()
        }
    }

    private val generatorsMap by lazy {
        config.generators
    }
    val curseforge by lazy {
        curseforgeFile.takeIf { it.exists() }?.let {
            json.decodeFromString(serializer, it.readText())
        } ?: runBlocking(MDCContext()) {
            val generatorsCurse = generatorsMap.filterValues { it is Generator.Curse } as Map<String, Generator.Curse>
            generateCurse(generatorsCurse)
        }
    }
    val forge by lazy {
        forgeFile.takeIf { it.exists() }?.let {
            json.decodeFromString(serializer, it.readText())
        } ?: runBlocking(MDCContext()) {
            val generatorsForge = generatorsMap.filterValues { it is Generator.Forge } as Map<String, Generator.Forge>
            generateForge(generatorsForge)
        }
    }
    val fabricIntermediaries by lazy {
        fabricIntermediariesFile.takeIf { it.exists() }?.let {
            json.decodeFromString(serializer, it.readText())
        } ?: runBlocking(MDCContext()) {
            val generatorsFabric = generatorsMap.filterValues { it is Generator.Fabric } as Map<String, Generator.Fabric>
            generateFabricIntermediaries(generatorsFabric)
        }
    }
    val fabricInstallers by lazy {
        fabricInstallersFile.takeIf { it.exists() }?.let {
            json.decodeFromString(serializer, it.readText())
        } ?: runBlocking(MDCContext()) {
            val generatorsFabric = generatorsMap.filterValues { it is Generator.Fabric } as Map<String, Generator.Fabric>
            generateFabricInstallers(generatorsFabric)
        }
    }
    val fabricLoaders by lazy {
        fabricLoadersFile.takeIf { it.exists() }?.let {
            json.decodeFromString(serializer, it.readText())
        } ?: runBlocking(MDCContext()) {
            val generatorsFabric = generatorsMap.filterValues { it is Generator.Fabric } as Map<String, Generator.Fabric>
            generateFabricLoaders(generatorsFabric)
        }
    }

    private suspend fun generateCurse(generatorsCurse: Map<String, Generator.Curse>): Map<String, String> =
        generatorsCurse.entries.fold(mapOf<String, String>()) { acc, (name, generator) ->
            acc + Poet.generateCurseforgeAutocomplete(
                section = generator.section,
                mcVersions = generator.mcVersions.toList()
            ).mapKeys { (key, _) ->
                "$name/$key"
            }
        }.also {
            curseforgeFile.writeText(json.encodeToString(serializer, it))
        }

    private suspend fun generateForge(generatorsForge: Map<String, Generator.Forge>): Map<String, String> =
        generatorsForge.entries.fold(mapOf<String, String>()) { acc, (name, generator) ->
            acc + Poet.generateForgeAutocomplete(
                mcVersionFilter = generator.mcVersions.toList()
            ).mapKeys { (key, _) ->
                "$name/$key"
            }
        }.also {
            forgeFile.writeText(json.encodeToString(serializer, it))
        }


    private suspend fun generateFabricIntermediaries(generatorsFabric: Map<String, Generator.Fabric>): Map<String, String>  =
        generatorsFabric.entries.fold(mapOf<String, String>()) { acc, (name, generator) ->
            acc + Poet.generateFabricIntermediariesAutocomplete(
                versionsFilter = generator.mcVersions.toList(),
                requireStable = generator.requireStable
            ).mapKeys { (key, _) ->
                "$name/$key"
            }
        }.also {
            fabricIntermediariesFile.writeText(json.encodeToString(serializer, it))
        }

    private suspend fun generateFabricLoaders(generatorsFabric: Map<String, Generator.Fabric>): Map<String, String>  =
        generatorsFabric.entries.fold(mapOf<String, String>()) { acc, (name, generator) ->
            acc + Poet.generateFabricLoadersAutocomplete(
                requireStable = generator.requireStable
            ).mapKeys { (key, _) ->
                "$name/$key"
            }
        }.also {
            fabricLoadersFile.writeText(json.encodeToString(serializer, it))
        }


    private suspend fun generateFabricInstallers(generatorsFabric: Map<String, Generator.Fabric>): Map<String, String> =
        generatorsFabric.entries.fold(mapOf<String, String>()) { acc, (name, generator) ->
            acc + Poet.generateFabricInstallersAutocomplete(
                requireStable = generator.requireStable
            ).mapKeys { (key, _) ->
                "$name/$key"
            }
        }.also {
            fabricInstallersFile.writeText(json.encodeToString(serializer, it))
        }


    suspend fun generate(configFile: File) {

        logger.info { "generating autocompletions into $cacheDir" }

        val config = if (configFile.exists()) {
            json.decodeFromString(
                Configuration.serializer(),
                configFile.readText()
            )
        } else Configuration()

        val generatorsMap = config.generators

        val generatorsCurse = generatorsMap.filterValues { it is Generator.Curse } as Map<String, Generator.Curse>
        val generatorsForge = generatorsMap.filterValues { it is Generator.Forge } as Map<String, Generator.Forge>
        val generatorsFabric = generatorsMap.filterValues { it is Generator.Fabric } as Map<String, Generator.Fabric>

        coroutineScope {
            launch(MDCContext()) {
                generateCurse(generatorsCurse)
            }
            launch(MDCContext()) {
                generateForge(generatorsForge)
            }
            launch(MDCContext()) {
                generateFabricIntermediaries(generatorsFabric)
            }
            launch(MDCContext()) {
                generateFabricInstallers(generatorsFabric)
            }
            launch(MDCContext()) {
                generateFabricLoaders(generatorsFabric)
            }
        }


        val generatorsSchemaFile = rootDir.resolve("schema/config.schema.json").apply {
            absoluteFile.parentFile.mkdirs()

            writeText(
                json.encodeToSchema(
                    Configuration.serializer()
                )
            )
        }

        configFile.writeText(
            json.encodeToString(
                Configuration.serializer(),
                config.copy(schema = generatorsSchemaFile.toRelativeUnixPath(configFile.parentFile))
            )
        )
    }

}