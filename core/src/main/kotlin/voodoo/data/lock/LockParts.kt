package voodoo.data.lock

import kotlinx.serialization.Serializable
import voodoo.data.DependencyType
import voodoo.data.OptionalData
import voodoo.data.Side

interface CommonLockModule {
    val id: String
    val path: String
    val name: String?
    val fileName: String?
    val side: Side
    val description: String?
    val optionalData: OptionalData?
    val dependencies: Map<String, DependencyType>
}

@Serializable
data class CommonLockComponent(
    override val id: String,
    override val path: String,
    override val name: String? = null,
    override val fileName: String? = null,
    override val side: Side = Side.BOTH,
    override val description: String? = null,
    override val optionalData: OptionalData? = null,
    override val dependencies: Map<String, DependencyType> = mapOf()
):CommonLockModule
