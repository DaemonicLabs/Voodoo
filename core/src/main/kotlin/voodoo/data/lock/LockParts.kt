package voodoo.data.lock

import kotlinx.serialization.Serializable
import voodoo.data.DependencyType
import voodoo.data.OptionalData
import voodoo.data.Side

interface CommonLockModule {
    val name: String?
    val fileName: String?
    val side: Side
    val description: String?
    val optionalData: OptionalData?
    val dependencies: Map<DependencyType, List<String>>
}

@Serializable
data class CommonLockComponent(
    override val name: String? = null,
    override val fileName: String? = null,
    override val side: Side = Side.BOTH,
    override val description: String? = null,
    override val optionalData: OptionalData? = null,
    override val dependencies: Map<DependencyType, List<String>> = mapOf()
):CommonLockModule
