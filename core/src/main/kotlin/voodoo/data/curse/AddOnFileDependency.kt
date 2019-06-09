package voodoo.data.curse

import kotlinx.serialization.Serializable

@Serializable
data class AddOnFileDependency(
    val addonId: ProjectID,
    @Serializable(with = CurseDependencyType.Companion::class)
    val type: CurseDependencyType
)