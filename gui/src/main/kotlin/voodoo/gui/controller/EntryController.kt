package voodoo.gui.controller

import javafx.collections.ObservableList
import voodoo.gui.model.EntryWrapper
import voodoo.gui.model.EntryModel
import tornadofx.*

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 * @version 1.0
 */
class EntryController : Controller() {
    val modpackController: ModpackController by inject()
    val entries: ObservableList<EntryWrapper> = observableList()
      // get() = modpackController.selectedModpack.entries.value ?: observableList()
    //FXCollections.observableArrayList<EntryWrapper>()

    val selectedEntry = EntryModel()

    init {
//        modpackController.selectedModpack.subscribe<MessageEvent> {
////            println(it.message.length)
//            entries.clear()
//            entries.addAll(modpackController.selectedModpack.entries.value)
//            selectedEntry.item = modpackController.selectedModpack.entries.value.first()
//        }
        selectedEntry.item = modpackController.selectedModpack.entries.value.firstOrNull()
    }
}