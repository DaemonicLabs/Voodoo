//package voodoo.gui.controller
//
//import javafx.collections.ObservableList
//import voodoo.gui.model.FlatEntryWrapper
//import voodoo.gui.model.EntryModel
//import tornadofx.*
//
///**
// * Created by nikky on 18/03/18.
// * @author Nikky
// * @version 1.0
// */
//class FlatEntryController : Controller() {
//    val modpackController: FlatModpackController by inject()
//    val tabController: TabController by inject()
//
//    val entries: ObservableList<FlatEntryWrapper> = observableList()
//      // get() = modpackController.selectedModpack.entries.value ?: observableList()
//    //FXCollections.observableArrayList<FlatEntryWrapper>()
//
//    val selectedEntry = EntryModel()
//
//    init {
////        modpackController.selectedModpack.subscribe<MessageEvent> {
//////            println(it.message.length)
////            entries.clear()
////            entries.addAll(modpackController.selectedModpack.entries.value)
////            selectedEntry.item = modpackController.selectedModpack.entries.value.first()
////        }
//        selectedEntry.item = modpackController.selectedModpack.entries.value.firstOrNull()
//
//
//        selectedEntry.itemProperty.addListener(ChangeListener { observable, oldValue, newValue ->
//            tabController.selectionModel.select(1)
//        })
//    }
//}