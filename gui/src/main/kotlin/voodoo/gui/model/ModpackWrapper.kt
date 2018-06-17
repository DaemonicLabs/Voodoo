package voodoo.gui.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javafx.beans.property.Property
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import tornadofx.*
import voodoo.data.flat.ModPack
import voodoo.gui.extensions.json

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 * @version 1.0
 */

class ModpackWrapper(modpack: ModPack) {
    @JsonIgnore
    val nameProperty = SimpleStringProperty(modpack.name)
    var name by nameProperty

    @JsonIgnore
    val titleProperty = SimpleStringProperty(modpack.title)
    var title by titleProperty

    @JsonIgnore
    val versionProperty = SimpleStringProperty(modpack.version)
    var version by versionProperty

    @JsonIgnore
    val authorsProperty = SimpleListProperty<SimpleStringProperty>(modpack.authors.map{ SimpleStringProperty(it) }.observable())
    var authors by authorsProperty

    @JsonIgnore
    val mcVersionProperty = SimpleStringProperty(modpack.mcVersion)
    var mcVersion by mcVersionProperty

    @JsonIgnore
    val forgeProperty = SimpleStringProperty(modpack.forge)
    var forge by forgeProperty

    @JsonIgnore
    val localDirProperty = SimpleStringProperty(modpack.localDir)
    var localDir by localDirProperty

    @JsonIgnore
    val minecraftDirProperty = SimpleStringProperty(modpack.minecraftDir)
    var minecraftDir by minecraftDirProperty


    // load EntryWrapper
    @JsonIgnore
    val entriesProperty = SimpleListProperty<EntryWrapper>(modpack.entries.map { EntryWrapper(it, modpack) }.observable())
    var entries by entriesProperty

    init {

    }

    override fun toString(): String {
        return "ModpackWrapper(${this.json})"
    }

    val modpack: ModPack
    get() {
        return ModPack(
                name = name,
                title = title,
                version = version,
                authors = authors.map { it.get() },
                mcVersion = mcVersion,
                forge = forge,
//                launch = launch, //TODO: make wrapper
//                userFiles = userFiles //TODO: make wrapper
                //TODO: make sure versions are saved
                //TODO: make sure features are saved

                entries = entries.map { it.entry },

                localDir = localDir,
                minecraftDir = minecraftDir
        )
    }

}

class ModpackModel : ItemViewModel<ModpackWrapper>() {
    val name = bind(ModpackWrapper::nameProperty)
    val title = bind(ModpackWrapper::titleProperty)
    val version = bind(ModpackWrapper::versionProperty)
    val authors: ObservableList<SimpleStringProperty> = bind(ModpackWrapper::authorsProperty)
    val mcVersion = bind(ModpackWrapper::mcVersionProperty)
    val forge = bind(ModpackWrapper::forgeProperty)
    val localDir = bind(ModpackWrapper::localDirProperty)
    val minecraftDir = bind(ModpackWrapper::minecraftDirProperty)
    val entries = bind(ModpackWrapper::entriesProperty)



    override fun onCommit(commits: List<Commit>) {
        commits.findChanged(name)?.let { println("Name changed from ${it.first} to ${it.second}")}
        commits.findChanged(title)?.let { println("Title changed from ${it.first} to ${it.second}")}
//        commits.findChanged(side)?.let { println("Side changed from ${it.first} to ${it.second}")}
//        commits.findChanged(id)?.let { println("ID changed from ${it.first} to ${it.second}")}

        onCommit()
    }



    private fun <T> List<Commit>.findChanged(ref: Property<T>): Pair<T, T>? {
        val commit = find { it.property == ref && it.changed }
        return commit?.let { (it.newValue as T) to (it.oldValue as T) }
    }


    override fun onCommit() {
        log.info("onCommit()")
//        item.compile()
    }

}