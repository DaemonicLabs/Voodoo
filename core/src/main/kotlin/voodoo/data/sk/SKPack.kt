package voodoo.data.sk

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import voodoo.data.sk.task.Task

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 */

@Serializable
data class SKPack(
    val title: String,
    val name: String,
    val version: String,
    val minimumVersion: Int,
    val librariesLocation: String,
    val objectsLocation: String,
    val gameVersion: String,
    @Optional val features: List<FeatureProperties> = emptyList(),
    val tasks: List<Task>,
    val versionManifest: VersionManifest,
    val launch: Launch
)