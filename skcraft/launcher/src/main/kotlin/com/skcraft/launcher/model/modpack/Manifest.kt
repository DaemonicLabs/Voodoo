// Generated by delombok at Sat Jul 14 05:49:42 CEST 2018
/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher.model.modpack

import com.skcraft.launcher.model.minecraft.VersionManifest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.KOutput
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import java.net.URL

@Serializable
class Manifest(
    @Optional
    var minimumVersion: Int = 0
) {
    @Optional
    var title: String? = null
    @Optional
    var name: String? = null
    @Optional
    var version: String? = null
    @Optional
    var baseUrl: URL? = null
    @Optional
    var librariesLocation: String? = null
    @Optional
    var objectsLocation: String? = null
    @Optional
    var gameVersion: String? = null
    @Optional
    @SerialName("launch")
    var launchModifier: LaunchModifier? = null
    @Optional
    var features: List<Feature> = emptyList()
    @Optional
    var tasks: List<FileInstall> = emptyList()
    @Optional
    var versionManifest: VersionManifest? = null

    fun updateName(name: String?) {
        if (name != null) {
            this.name = name
        }
    }

    fun updateTitle(title: String?) {
        if (title != null) {
            this.title = title
        }
    }

    fun updateGameVersion(gameVersion: String?) {
        if (gameVersion != null) {
            this.gameVersion = gameVersion
        }
    }

    @Serializer(forClass=Manifest::class)
    companion object : KSerializer<Manifest> {
        val MIN_PROTOCOL_VERSION = 2

        override fun save(output: KOutput, obj: Manifest) {
            val elemOutput = output.writeBegin(serialClassDesc)
            obj.title?.let { title ->
                elemOutput.writeStringElementValue(serialClassDesc, 1, title)
            }
            obj.name?.let { name ->
                elemOutput.writeStringElementValue(serialClassDesc, 2, name)
            }
            obj.version?.let { version ->
                elemOutput.writeStringElementValue(serialClassDesc, 3, version)
            }
                elemOutput.writeIntElementValue(serialClassDesc, 0, obj.minimumVersion)
            obj.baseUrl?.let { baseUrl ->
                elemOutput.writeStringElementValue(serialClassDesc, 4, baseUrl.toString())
            }
            obj.librariesLocation?.let { librariesLocation ->
                elemOutput.writeStringElementValue(serialClassDesc, 5, librariesLocation)
            }
            obj.objectsLocation?.let { objectsLocation ->
                elemOutput.writeStringElementValue(serialClassDesc, 6, objectsLocation)
            }
            obj.gameVersion?.let { gameVersion ->
                elemOutput.writeStringElementValue(serialClassDesc, 7, gameVersion)
            }
            obj.launchModifier?.let { launchModifier ->
                elemOutput.writeElement(serialClassDesc, 8)
                elemOutput.write(LaunchModifier::class.serializer(), launchModifier)
            }
            obj.features.takeUnless { it.isEmpty() }?.let { features ->
                elemOutput.writeElement(serialClassDesc, 9)
                elemOutput.write(Feature::class.serializer().list, features)
            }
            obj.tasks.takeUnless { it.isEmpty() }?.let { tasks ->
                elemOutput.writeElement(serialClassDesc, 10)
                elemOutput.write(FileInstall::class.serializer().list, tasks)
            }
            obj.versionManifest?.let {versionManifest ->
                elemOutput.writeElement(serialClassDesc, 11)
                elemOutput.write(VersionManifest::class.serializer(), versionManifest)
            }
            elemOutput.writeEnd(serialClassDesc)
        }
        
    }
}
