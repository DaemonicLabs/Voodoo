package voodoo.autocomplete

import mu.KotlinLogging
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
import voodoo.fabric.FabricUtil
import voodoo.forge.ForgeUtil

object Autocomplete {
    private val logger = KotlinLogging.logger {}

    suspend fun generateCurseforgeAutocomplete(
        categories: List<String> = emptyList(),
        section: CurseSection,
        mcVersions: List<String>
    ) : Map<String, String> {
        val slugIdMap = requestSlugIdMap(
            gameVersions = mcVersions.toList(),
            categories = categories,
            section = section.sectionName
        )

        return slugIdMap.mapValues { (_, id) ->
            id.toString()
        }.toSortedMap()
    }

    suspend fun generateForgeAutocomplete(
        mcVersionFilter: List<String>
    ) : Map<String, String> {
        val mcVersionsMap = ForgeUtil.mcVersionsMap(filter = mcVersionFilter)

        val flatVersions = mcVersionsMap.flatMap { (versionIdentifier, numbers) ->
            numbers.map {  (buildIdentifier, fullversion) ->
                "$versionIdentifier/$buildIdentifier" to fullversion
            }
        }

        val allVersions = mcVersionsMap.flatMap { it.value.values }
        val promos = ForgeUtil.promoMap()
        val filteredPromos = promos.filterValues { version ->
            allVersions.contains(version)
        }

        return filteredPromos + flatVersions
    }

    suspend fun generateFabricIntermediariesAutocomplete(
        requireStable: Boolean = false,
        versionsFilter: List<String> = emptyList()
    ): Map<String, String> {
        return FabricUtil.getIntermediaries()
            .run {
                if(versionsFilter.isNotEmpty()) filter { it.version in versionsFilter } else this
            }
            .run {
                if(requireStable) filter { it.stable } else this
            }
            .associate {
                it.version to it.version
            }
            .toSortedMap()
    }

    suspend fun generateFabricLoadersAutocomplete(
        requireStable: Boolean = false
    ): Map<String, String> {
        return FabricUtil.getLoaders().filter {
            !requireStable || it.stable
        }.associate {
            it.version to it.version
        }.toSortedMap()
    }
    suspend fun generateFabricInstallersAutocomplete(
        requireStable: Boolean = false
    ): Map<String, String> {
        return FabricUtil.getInstallers().filter {
            !requireStable || it.stable
        }.associate {
            it.version to it.version
        }.toSortedMap()
    }
    suspend fun requestSlugIdMap(
        section: String,
        categories: List<String>? = null,
        gameVersions: List<String>? = null
    ): Map<String, ProjectID> =
        CurseClient.graphQLRequest(
            section = section,
            categories = categories,
            gameVersions = gameVersions
        ).map { (id, slug) ->
            slug to id
        }.toMap()
}