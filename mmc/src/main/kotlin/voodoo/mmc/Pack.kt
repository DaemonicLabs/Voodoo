package voodoo.mmc

import voodoo.data.Launch

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 * @version 1.0
 */

data class Pack(
        val title: String,
        val name: String,
        val version: String,
        val minimumVersion: Int,
        val librariesLocation: String,
        val objectsLocation: String,
        val gameVersion: String,
        val features: List<Feature> = emptyList(),
        val tasks: List<Task>,
        val versionManifest: VersionManifest,
        val launch: Launch
)