package voodoo.flatten

import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.util.readYaml
import java.io.File
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

/**
 * Created by nikky on 13/06/18.
 * @author Nikky
 */


fun NestedPack.flatten(parentFile: File): ModPack {
    return ModPack(
            name = name,
            title = title,
            version = version,
            authors = authors,
            forge = forge,
            mcVersion = mcVersion,
            userFiles = userFiles,
            entries = root.flatten(parentFile),
            versionCache = File(versionCache.path),
            featureCache = File(featureCache.path),
            localDir = localDir,
            minecraftDir = minecraftDir
    )
}

private val default = NestedEntry()

fun NestedEntry.flatten(parentFile: File): List<Entry> {
    flatten("", parentFile)
    return this.entries.map { it ->
        Entry().apply {
            for (prop in NestedEntry::class.memberProperties) {
                if (prop is KMutableProperty<*>) {
                    val value = prop.get(it)
                    val otherPop = (Entry::class.memberProperties.find { it.name == prop.name })
                            as? KMutableProperty<*>
                            ?: continue
                    otherPop.setter.call(this, value)
                }
            }
        }
    }
}

private fun NestedEntry.flatten(indent: String, parentFile: File) {
    var parent = parentFile
    val toDelete = mutableListOf<NestedEntry>()
    include?.let {
        println("loading $include")
        val includeFile = parentFile.resolve(it)
        val includeEntry = includeFile.readYaml<NestedEntry>()

        for (prop in NestedEntry::class.memberProperties) {
            if (prop is KMutableProperty<*>) {
                val includeValue = prop.get(includeEntry)
                val thisValue = prop.get(this)
                val defaultValue = prop.get(default)

                if (thisValue == defaultValue) {
                    println("setting ${prop.name}")
                    prop.setter.call(this, includeValue)
                }
            }
        }
        parent = includeFile.parentFile
        println("loaded $includeFile")
        include = null
    }

    entries.forEach { entry ->
        for (prop in NestedEntry::class.memberProperties) {
            if (prop is KMutableProperty<*>) {
                val otherValue = prop.get(entry)
                val thisValue = prop.get(this)
                val defaultValue = prop.get(default)
                if (otherValue == defaultValue && thisValue != defaultValue) {
                    if (prop.name != "entries" && prop.name != "template") {
                        // clone maps
                        when (thisValue) {
                            is MutableMap<*, *> -> {
                                val map = thisValue.toMutableMap()
                                // copy lists
                                map.forEach { k, v ->
                                    if (v is List<*>) {
                                        map[k] = v.toList()
                                    }
                                }
                                prop.setter.call(entry, map)
                            }
                            is Set<*> -> prop.setter.call(entry, thisValue.toSet())
                            else ->
                                prop.setter.call(entry, thisValue)
                        }
                    }
                }
            }
        }

        entry.flatten("$indent|  ", parent)
        if (entry.entries.isNotEmpty()) {
            toDelete += entry
        }
        entry.entries.forEach { entries += it }
        entry.entries = listOf()
    }
    entries = entries.filter { !toDelete.contains(it) }
}