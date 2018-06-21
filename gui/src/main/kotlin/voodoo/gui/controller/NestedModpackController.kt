package voodoo.gui.controller

import javafx.beans.property.SimpleListProperty
import tornadofx.*
import voodoo.data.flat.ModPack
import voodoo.data.nested.NestedPack
import voodoo.gui.model.*
import voodoo.util.readJson
import voodoo.util.readYaml
import java.io.File

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 * @version 1.0
 */
class NestedModpackController : Controller() {
    val tabController: TabController by inject()

    val modpacks = SimpleListProperty<NestedModpackWrapper>(observableList())

    val selectedModpack = NestedModpackModel()
    val selectedRootEntry = NestedEntryModel()

    init {
        // iterate though json files and try to load them
        val path = System.getProperty("user.dir")
        val files = File(path).listFiles { dir, name ->
            name.endsWith(".yaml")
                    && !name.endsWith(".include.yaml")
                    && !name.endsWith(".lock.json")
                    && !name.endsWith(".features.json")
                    && !name.endsWith(".versions.json")
        }

        files.forEach {
            log.info("try loading $it")
            try {
                val modpack = it.readYaml<NestedPack>()
                modpacks.add(NestedModpackWrapper(modpack))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        modpacks.firstOrNull()?.apply {
            selectedModpack.item = this
            selectedRootEntry.item = this.rootEntry
        }

        selectedModpack.itemProperty.addListener(ChangeListener { observable, oldValue, newValue ->
            //            log.info("change of ${oldValue.name} to ${newValue.name}")
            tabController.selectionModel.select(0)
            selectedRootEntry.item = newValue.rootEntry
        })

    }
}