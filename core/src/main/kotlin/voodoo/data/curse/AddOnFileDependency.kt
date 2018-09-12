package voodoo.data.curse

import kotlinx.serialization.Serializable

@Serializable
data class AddOnFileDependency(
        val addOnId: ProjectID,
        val type: DependencyType
)