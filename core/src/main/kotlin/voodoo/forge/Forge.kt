package voodoo.forge

import aballano.kotlinmemoization.tuples.Quadruple
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogging

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */
object Forge : KLogging() {
    var data: ForgeData = getForgeData()

//    fun getForge(forgeVersion: String, mcVersion: String/*, spongeEntry: Entry?*/): Pair<Entry, String> {
//        val (url, filename, longVersion, version) = getForgeUrl(forgeVersion, mcVersion)
//
//        val entry = Entry(
//                provider = "DIRECT",
//                name = "Minecraft Forge",
//                url = url,
//                fileName = filename
////                    packageType = PackageType.LOADER,
////                internal = EntryInternal(
////                        basePath = "loaders",
////                        targetPath = ".",
////                        path = ".",
////                        cacheRelpath = "FORGE/$longVersion"
////                )
//        )
//        return Pair(entry, version)
//    }

    fun getForgeBuild(version: String, mcVersion: String): Int {
        return if (version.equals("recommended", true) || version.equals("latest", true)) {
            val promoVersion = "$mcVersion-${version.toLowerCase()}"
            data.promos[promoVersion]!!
        } else {
            version.toInt()
        }
    }

    fun getForgeUrl(version: String, mcVersion: String): Quadruple<String, String, String, String> {
        var versionStr: String
        if (version.equals("recommended", true) || version.equals("latest", true)) {
            val promoVersion = "$mcVersion-${version.toLowerCase()}"
            versionStr = data.promos[promoVersion]?.toString() ?: ""
        } else {
            if (data.number.containsKey(version)) {
                versionStr = version
            } else {
                versionStr = data.promos[version]?.toString() ?: ""
                if (versionStr.isBlank()) {
                    var versionList = data.branches.getOrDefault(version, emptyList())
                    if (versionList.isEmpty()) {
                        versionList = data.mcversion.getOrDefault(version, emptyList())
                    }

                    if (versionList.isNotEmpty()) {
                        versionList = versionList.filter { i -> data.number[i.toString()]?.mcversion == mcVersion }
                    }
                    if (versionList.isEmpty()) {
                        throw IllegalArgumentException("forge value is invalid")
                    }
                    versionStr = versionList.max().toString()
                }
            }
        }
        val webpath = data.webpath
        val artifact = data.number.get(versionStr)!!
        val mcversion = artifact.mcversion
        val forgeVersion = artifact.version
        val branch = artifact.branch
        var longVersion = "$mcversion-$forgeVersion"
        if (branch != null) {
            longVersion += "-$branch"
        }
        val fileName = "forge-$longVersion-installer.jar" // "forge-mcversion-$forgeVersion(-$branch)/installer.jar"
        val url = "$webpath/$longVersion/$fileName"

        return Quadruple(url, fileName, longVersion, forgeVersion)
    }

    fun getForgeData(): ForgeData {
        val (_, _, result) = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/json"
                .httpGet().responseString()
        return when (result) {
            is Result.Success -> {
                val mapper = jacksonObjectMapper() // Enable YAML parsing
                mapper.registerModule(KotlinModule()) // Enable Kotlin support
                mapper.readValue(result.component1()!!)
            }
            is Result.Failure -> {
                throw Exception("cannot get forge data ${result.error}")
            }
        }
    }

}

data class ForgeData(
        val adfocus: String,
        val artifact: String,
        val homepage: String,
        val webpath: String,
        val name: String,
        val branches: Map<String, List<Int>>,
        val mcversion: Map<String, List<Int>>,
        val number: Map<String, Artifact>,
        val promos: Map<String, Int>
)

data class Artifact(
        val branch: String?,
        val build: Int,
        val files: List<List<String>>, //file, extension, checksum
        val mcversion: String,
        val modified: Long,
        val version: String
)
