package voodoo.gui.controller

import javafx.collections.ObservableList
import voodoo.gui.model.FlatEntryWrapper
import voodoo.gui.model.EntryModel
import tornadofx.*
import voodoo.gui.model.NestedEntryModel
import voodoo.gui.model.NestedEntryWrapper

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 * @version 1.0
 */
class NestedEntryController : Controller() {
    val modpackController: NestedModpackController by inject()
    val tabController: TabController by inject()

    val selectedEntry = NestedEntryModel()

    init {
//        selectedEntry.item = modpackController.selectedModpack.rootEntry.value


        modpackController.selectedRootEntry.itemProperty.addListener(ChangeListener { observable, oldValue, newValue ->
            log.info("selecting new first value ")
//            selectedEntry.item = newValue.entries.first() //TODO: first entry that doesn ot have subentries
        })

        selectedEntry.itemProperty.addListener(ChangeListener { observable, oldValue, newValue ->
            tabController.selectionModel.select(1)
        })

        selectedEntry.itemProperty.addListener(ChangeListener { observable, oldValue, newValue ->
            log.info("change of ${oldValue?.name} to ${newValue?.name}")
        })

    }
}