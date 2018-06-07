package voodoo.data.sk

import voodoo.data.sk.task.Task

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 */

data class SKPack(
        val title: String,
        val name: String,
        val version: String,
        val minimumVersion: Int,
        val librariesLocation: String,
        val objectsLocation: String,
        val gameVersion: String,
        val features: List<SKFeature> = emptyList(),
        val tasks: List<Task>,
        val versionManifest: VersionManifest,
        val launch: Launch
)