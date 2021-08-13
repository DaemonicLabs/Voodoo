package voodoo.labrinth

import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import mu.KotlinLogging
import voodoo.util.json
import voodoo.util.useClient

enum class SideType {
    required,
    unsupported,
    optional,
    unknown,
}

enum class FacetCategory {
    worldgen,
    technology,
    food,
    magic,
    storage,
    library,
    adventure,
    utility,
    decoration,
    misc,
    equipment,
    cursed,
    fabric,
    forge,
}

object Facet {
    fun category(category: FacetCategory) = "categories:${category.name}"
    fun version(version: String) = "versions:$version"
    fun license(license: String) = "license:${license}"
    fun clientSide(side: SideType) = "client_side:${side.name}"
    fun serverSide(side: SideType) = "server_side:${side.name}"
}

@Serializable
data class SearchResponse(
    val hits: List<ModSearchResult>,
    val offset: Int,
    val limit: Int,
    val total_hits: Int,
)

@Serializable
data class ModSearchResult(
    val mod_id: ModId, // 	The id of the mod; prefixed with local-
    val slug: String,
    val author: String, // 	The username of the author of the mod
    val title: String, // 	The name of the mod
    val description: String, // 	A short description of the mod
    val categories: List<String>, // of strings 	A list of the categories the mod is in
    val versions: List<String>, // of strings 	A list of the minecraft versions supported by the mod
    val downloads: Int, // 	The total number of downloads for the mod
    val follows: Int,
    val page_url: String, // 	A link to the mod's main page;
    val icon_url: String, // 	The url of the mod's icon
    val author_url: String, // 	The url of the mod's author
    val date_created: String, // TODO: datetime // 	The date that the mod was originally created
    val date_modified: String, // TODO: datetime // 	The date that the mod was last modified
    val latest_version: String, // 	The latest version of minecraft that this mod supports
    val license: String, // 	The id of the license this mod follows
    val client_side: SideType, // 	The side type id that this mod is on the client
    val server_side: SideType, // 	The side type id that this mod is on the server
    val host: String, // 	The host that this mod is from, always modrinth
)

@Serializable
data class ModResult(
    val id: String, // The ID of the mod, encoded as a base62 string
    val slug: String, // 	The slug of a mod, used for vanity URLs
    val team: String, // id 	The id of the team that has ownership of this mod
    val title: String, // 	The title or name of the mod
    val description: String, // 	A short description of the mod
    val body: String, // 	A long form description of the mod.
    val body_url: String?, // 	DEPRECATED The link to the long description of the mod
    val published: String, // 	The date at which the mod was first published
    val updated: String, // 	The date at which the mod was updated
    val status: String, // TODO: status enum // 	The status of the mod - approved, rejected, draft, unlisted, processing, or unknown
    val license: License, // 	The license of the mod
    val client_side: SideType, // 	The support range for the client mod - required, optional, unsupported, or unknown
    val server_side: SideType, // 	The support range for the server mod - required, optional, unsupported, or unknown
    val downloads: Int, // 	The total number of downloads the mod has
    val followers: Int,
    val categories: List<String>, // 	A list of the categories that the mod is in
    val versions: List<VersionId>, // A list of ids for versions of the mod
    val icon_url: String?, // 	The URL of the icon of the mod
    val issues_url: String?, // 	An optional link to where to submit bugs or issues with the mod
    val source_url: String?, // 	An optional link to the source code for the mod
    val wiki_url: String?, // 	An optional link to the mod's wiki page or other relevant information
    val discord_url: String?, // 	An optional link to the mod's discord
    val donation_urls: List<String>, // of donation links 	An optional list of all donation links the mod has
)

@JvmInline
@Serializable
value class VersionId(val id: String)

@JvmInline
@Serializable
value class ModId(val id: String)

@JvmInline
@Serializable
value class UserId(val id: String)

@Serializable
data class VersionResult(
    val id: VersionId, // id 	The ID of the version, encoded as a base62 string
    val mod_id: ModId, // id 	The ID of the mod this version is for
    val author_id: UserId, // id 	The ID of the author who published this version
    val featured: Boolean, // id 	Whether the version is featured or not
    val name: String, // 	The name of this version
    val version_number: String, // 	The version number. Ideally will follow semantic versioning
    val changelog: String?, // 	The changelog for this version of the mod.
    val changelog_url: String?, // 	DEPRECATED A link to the changelog for this version of the mod
    val date_published: String, //TODO: dateime // 	The date that this version was published
    val downloads: Int, // 	The number of downloads this specific version has
    val version_type: String, // 	The type of the release - alpha, beta, or release
    val files: List<VersionFile>, // of VersionFiles 	A list of files available for download for this version
    val dependencies: List<VersionDependency>, // A list of specific versions of mods that this version depends on
    val game_versions: List<String>, // of game versions 	A list of versions of Minecraft that this version of the mod supports
    val loaders: List<String>, // of mod loaders 	The mod loaders that this version supports
)

@Serializable
data class VersionFile(
    val hashes: Map<String, String>, // 	A map of hashes of the file. The key is the hashing algorithm and the value is the string version of the hash.
    val url: String, //	A direct link to the file
    val filename: String, // 	The name of the file
    val primary: Boolean,
)
@Serializable
data class VersionDependency(
    val version_id: VersionId,
    val dependency_type: String,
)

@Serializable
data class License(
    val id: String, // 	The license id of a mod, retrieved from the licenses get route
    val name: String, // 	The long for name of a license
    val url: String?, // 	The URL to this license
)

object LabrinthClient {
    private val logger = KotlinLogging.logger {}
    private const val PREFIX = "https://api.modrinth.com"

    suspend fun search(
        vararg facets: List<String>,
        limit: Int = 100,
    ): List<ModSearchResult> {
        return withContext(MDCContext() + Dispatchers.IO) {
            val url = "${PREFIX}/api/v1/mod"

            val facetQuery = json.encodeToString(
                ListSerializer(ListSerializer(String.serializer())),
                listOf(*facets)
            )
            val hits = useClient { client ->
                val hits = mutableListOf<ModSearchResult>()
                var offset = 0
                do {
                    val searchResponse = client.get<SearchResponse> {
                        url {
                            url(url)
                            parameter("facets", facetQuery)
                            parameter("limit", limit)
                            parameter("offset", offset)
                        }
                    }
                    hits += searchResponse.hits

                    val currentHits = searchResponse.hits.size
                    val continueRequests = searchResponse.offset + currentHits < searchResponse.total_hits
                    offset += limit
                } while (continueRequests)

                hits.toList()
            }

//            hits.forEach {
//                logger.info { it }
//            }
            hits
        }
    }

    suspend fun get(slug: String): ModResult {

        return withContext(MDCContext() + Dispatchers.IO) {
            val url = "${PREFIX}/api/v1/mod/$slug"

            val mod = useClient { client ->
                client.get<ModResult>(url)
            }
            mod
        }
    }

    suspend fun getModsByIds(ids: List<ModId>): List<ModResult> {
        return withContext(MDCContext() + Dispatchers.IO) {
            val url = "${PREFIX}/api/v1/mods"

            val mod = useClient { client ->
                client.get<List<ModResult>> {
                    url {
                        url(url)
                        parameter("ids", json.encodeToString(ListSerializer(ModId.serializer()), ids))
                    }
                }
            }
            mod
        }
    }

    suspend fun versions(slug: String): List<VersionId> {
        return withContext(MDCContext() + Dispatchers.IO) {
            val url = "${PREFIX}/api/v1/mod/$slug/version"

            useClient { client ->
                client.get<List<VersionId>>(url)
            }
        }
    }

    suspend fun version(versionId: VersionId): VersionResult {
        return withContext(MDCContext() + Dispatchers.IO) {
            val url = "${PREFIX}/api/v1/version/${versionId.id}"

            logger.trace { url }

            useClient { client ->
                client.get<VersionResult>(url)
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val hits = search(
                listOf(Facet.category(FacetCategory.fabric)),
                listOf(Facet.category(FacetCategory.cursed)),
                listOf(
                    Facet.version("1.17.1"),
                    Facet.version("1.17")
                ),
            )

            hits.forEach {
//                logger.info { it }

                val mod = get(it.slug)
                logger.info { mod.slug }
//                logger.info { mod.versions }

                mod.versions.forEach { versionId ->
                    val version = version(versionId)
                    logger.info { "${mod.title}: '${version.name}' '${version.version_number}'" }
                    version.dependencies.forEach { versionDependency ->
                        val dep = version(versionDependency.version_id)
                        val depMod = getModsByIds(listOf(dep.mod_id)).first()
                        logger.info { "  ${versionDependency.dependency_type}: '${depMod.title}' '${dep.name}' '${dep.version_number}'" }
                    }
                }
            }
        }
    }
}