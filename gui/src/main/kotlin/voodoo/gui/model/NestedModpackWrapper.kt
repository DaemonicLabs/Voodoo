package voodoo.gui.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javafx.beans.property.Property
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import tornadofx.*
import voodoo.data.flat.ModPack
import voodoo.data.nested.NestedPack
import voodoo.gui.extensions.json

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 * @version 1.0
 */

class NestedModpackWrapper(modpack: NestedPack) {
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


    // load FlatEntryWrapper
    @JsonIgnore
    val rootProperty = SimpleObjectProperty<NestedEntryWrapper>(NestedEntryWrapper(modpack.root, null))
    var rootEntry by rootProperty

    init {

    }

    override fun toString(): String {
        return "NestedModpackWrapper(${this.json})"
    }

    val modpack: NestedPack
    get() {
        return NestedPack(
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



                localDir = localDir,
                minecraftDir = minecraftDir,
                root = rootEntry.entry
        )
    }

}

class NestedModpackModel : ItemViewModel<NestedModpackWrapper>() {
    val name = bind(NestedModpackWrapper::nameProperty)
    val title = bind(NestedModpackWrapper::titleProperty)
    val version = bind(NestedModpackWrapper::versionProperty)
    val authors: ObservableList<SimpleStringProperty> = bind(NestedModpackWrapper::authorsProperty)
    val mcVersion = bind(NestedModpackWrapper::mcVersionProperty)
    val forge = bind(NestedModpackWrapper::forgeProperty)
    val localDir = bind(NestedModpackWrapper::localDirProperty)
    val minecraftDir = bind(NestedModpackWrapper::minecraftDirProperty)
    val rootEntry = bind(NestedModpackWrapper::rootProperty)



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