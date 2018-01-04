package moe.nikky.builder

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import khttp.get

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */
class Forge {
    companion object {
        var data: ForgeData = getForgeData()

        fun getSponge(spongeVersion: String): Entry {
            val entry = Entry(
                    provider = Provider.MAVEN,
                    name = "Sponge Forge",
                    remoteRepository = "https://repo.spongepowered.org/maven/",
                    group = "org.spongepowered",
                    artifact = "spongeforge",
                    version = spongeVersion,
                    //packageType = PackageType.MOD,
                    path = "mods",
                    side = Side.SERVER)
            return entry
        }

        fun getForge(forgeVersion: String, mcVersion: List<String>, spongeEntry: Entry?): Entry {
            var version = forgeVersion
            if (spongeEntry != null) {
                val spongeVersion = spongeEntry.version
                println(spongeVersion)
                version = spongeVersion.split("-")[1]
            }
            val (url, filename, longVersion) = getForgeUrl(version, mcVersion)
            //TODO CACHE DIR
            val cacheDir = "~/.cache/voodoo/forge"
            val entry = Entry(
                    provider = Provider.DIRECT,
                    name = "Minecraft Forge",
                    url = url,
                    fileName = filename,
//                    packageType = PackageType.LOADER,
                    basePath = "loaders"
            )
            return entry
        }

        fun getForgeUrl(version: String, mcVersionList: List<String>): Triple<String, String, String> {
            val mcVersion = mcVersionList.first()
            var versionStr = ""
            if (version.equals("recommended", true) || version.equals("latest", true)) {
                val promoVersion = "$mcVersion-${version.toLowerCase()}"
                versionStr = data.promos.getOrDefault(promoVersion, null)?.toString() ?: ""
            } else {
                versionStr = data.promos.getOrDefault(version, null)?.toString() ?: ""
                if (versionStr.isBlank()) {
                    var versionList: List<Int> = emptyList()
                    versionList = data.branches.getOrDefault(version, null) ?: emptyList()
                    if (versionList.isEmpty()) {
                        versionList = data.mcversion.getOrDefault(version, null) ?: emptyList()
                    }

                    if (versionList.isNotEmpty()) {
                        versionList = versionList.filter { i -> data.number.get(i.toString())?.mcversion == mcVersion }
                    }
                    if (versionList.isEmpty()) {
                        throw IllegalArgumentException("forge value is invalid")
                    }
                    versionStr = versionList.max().toString()
                }
            }
            val webpath = data.webpath
            val artifact = data.number.get(versionStr) !!
            val mcversion = artifact.mcversion
            val forgeVersion = artifact.version
            val branch = artifact.branch
            var longVersion = "$mcVersion-$forgeVersion"
            if(branch != null) {
                longVersion += "-$branch"
            }
            val fileName = "forge-$longVersion-installer.jar"
            val url = "$webpath/$longVersion/$fileName"

            return Triple(url, fileName, longVersion)
        }

        fun getForgeData(): ForgeData {
            val r = get("http://files.minecraftforge.net/maven/net/minecraftforge/forge/json")
            if (r.statusCode == 200) {
                val mapper = jacksonObjectMapper() // Enable YAML parsing
                mapper.registerModule(KotlinModule()) // Enable Kotlin support

                val state = mapper.readValue<ForgeData>(r.text)
                return state
            }

            throw Exception("cannot get forge data")
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
        val files: List<Any>,
        val mcversion: String,
        val modified: Long,
        val version: String
)
