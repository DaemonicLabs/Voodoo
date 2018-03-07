package voodoo.builder.provider

import aballano.kotlinmemoization.memoize
import aballano.kotlinmemoization.tuples.Quintuple
import mu.KLogging
import voodoo.builder.Job
import voodoo.builder.JobManager
import voodoo.builder.JobTracker
import voodoo.builder.curse.DependencyType
import voodoo.builder.data.Entry
import voodoo.builder.data.Modpack
import voodoo.builder.data.Side
import voodoo.builder.data.Feature
import java.io.File

/**
 * Created by nikky on 04/01/18.
 * @author Nikky
 * @version 1.0
 */

enum class Provider(val base: ProviderBase) {
    CURSE(CurseProviderThing()),
    DIRECT(DirectProviderThing()),
    LOCAL(LocalProviderThing()),
    JENKINS(JenkinsProviderThing()),
    DUMMY(DummyProviderThing()),
    JSON(UpdateJsonProviderThing())
}

//private var processedFeatures = listOf<String>() //TODO: move into modpack-shared object
//private val processedFunctions = mutableMapOf<String, List<String>>() //TODO: move into modpack-shared object

abstract class ProviderBase(val name: String = "abstract Provider") {
//    private var functions = listOf<Quintuple<String, (Entry) -> Boolean, (Entry, Modpack) -> Unit, Boolean, String>>()

    companion object : KLogging() {
//        private val requirmentWarning = mutableMapOf<String, Boolean>()
//
//        fun resetWarnings() {
//            requirmentWarning.clear()
//        }
    }

    val jobManager: JobManager = JobManager(name)

    init {
        register("setFeatureName",
                { it.feature != null && it.feature!!.name.isBlank() },
                { e, _ ->
                    e.feature!!.name = e.name
                }
        )

        register("postSetName",
                { it.name.isNotBlank() },
                { e, _ ->
                    logger.info("run after name is set ${e.name}")
                }
        )

        register("checkDuplicate",
                { it.name.isNotBlank() },
                { e, m ->
                    val duplicates = m.mods.entries.filter { it != e && it.name == e.name }
                    if (duplicates.isNotEmpty()) {
                        println()
                        logger.error("duplicates: {}", duplicates)
                        System.exit(2)
                    }
                },
                requires = "postSetName"
        )

        register2("postResolveDependencies",
                //TODO: stages
                //match all noncurse and all curse dependencies with dependencies resolved
                check@{ e, m ->
                    if(e.name.isBlank()) return@check false
                    if(e.provider != Provider.CURSE) return@check true
                    return@check m.tracker.isProcessed(e.name, "resolveDependencies")
                },
                { e, _ ->
                    logger.info("run after resolveDependencies ${e.name}")
                }
        )

        register("resolveProvides",
                { it.name.isNotBlank() },
                { e, m ->
                    logger.info("provides starting: $e")
                    m.mods.entries.forEach { entry ->
                        entry.dependencies.forEach { type, dependencies ->
                            var provides = e.provides[type] ?: listOf()
                            dependencies.forEach { dependency ->
                                if (e.name == dependency && !provides.contains(entry.name)) {
                                    logger.info("${e.name} provides $type ${entry.name}")
                                    provides += entry.name
                                }
                            }
                            e.provides[type] = provides
                        }
                    }
                    logger.info("provides finished: $e")
                },
                requires = "postResolveDependencies"
        )

        register2("resolveFeatureDependencies",
                { e, m ->
                    e.name.isNotBlank() && !m.tracker.processedFeatures.contains(e.name) },
                jobManager::resolveFeatureDependencies,
                repeatable = true,
                requires = "resolveProvides"
        )

        register("resolveOptional",
                { true },
                { e, m ->
                    e.optional = isOptional(e, m)
                },
                requires = "resolveFeatureDependencies"
        )

        register("setCachePath",
                { it.internal.cachePath.isBlank() && it.internal.cacheRelpath.isNotBlank() },
                { e, m ->
                    e.internal.cachePath = File(m.internal.cacheBase).resolve(e.internal.cacheRelpath).path
                }
        )

        register("resolvePath",
                { it.internal.filePath.isBlank() && it.internal.targetPath.isNotBlank() && it.fileName.isNotBlank() },
                { e, _ ->
                    var path = e.internal.targetPath
                    // side
                    if (path.startsWith("mods")) {
                        val side = when (e.side) {
                            Side.CLIENT -> "_CLIENT"
                            Side.SERVER -> "_SERVER"
                            Side.BOTH -> ""
                        }
                        if (side.isNotBlank()) {
                            path = "$path/$side"
                        }
                    }
                    e.internal.path = path
                    e.internal.filePath = File(e.internal.basePath, e.internal.path).resolve(e.fileName).path
                }
        )

        register("resolveTargetFilePath",
                { it.internal.targetFilePath.isBlank() && it.internal.targetPath.isNotBlank() && it.fileName.isNotBlank() },
                { e, _ ->
                    e.internal.targetFilePath = File(e.internal.targetPath).resolve(e.fileName).path
                }
        )
    }

    fun register2(label: String, condition: (Entry, Modpack) -> Boolean, execute: (Entry, Modpack) -> Unit, repeatable: Boolean = false, requires: String = "", force: Boolean = false) {
        val job = Job(label, condition, execute, repeatable, requires)
        logger.info("registering $label to $this")
        jobManager.register(label, job)
//        val duplicate = functions.find { it.first == label }
//        if (duplicate != null) {
//            if (force) {
//                functions -= duplicate
//            } else {
//                logger.warn("cannot register duplicate $label")
//                return
//            }
//        }
//        functions += Quintuple(label, condition, execute, repeatable, requires)
    }
    fun register(label: String, condition: (Entry) -> Boolean, execute: (Entry, Modpack) -> Unit, repeatable: Boolean = false, requires: String = "", force: Boolean = false) {
        val job = Job(label, {entry: Entry, modpack: Modpack -> condition(entry)}, execute, repeatable, requires)
        logger.info("registering $label to $this")
        jobManager.register(label, job)
    }

    private fun isOptionalCall(entry: Entry, modpack: Modpack): Boolean {
        logger.info("test optional of ${entry.name}")
//        logger.info(entry.toString())
        var result = entry.transient || entry.optional
        if (result) return result
        for ((depType, entryList) in entry.provides) {
            if (depType != DependencyType.REQUIRED) continue
            if (entryList.isEmpty()) return false
            logger.info("type: $depType list: $entryList")
            for (entryName in entryList) {
                val providerEntry = modpack.mods.entries.firstOrNull { it.name == entryName }!!
                val tmpResult = isOptional(providerEntry, modpack)
                if (!tmpResult) return false
            }
        }
        return true
    }

    val isOptional = ::isOptionalCall.memoize()
}