//package moe.nikky.voodoo.view
//
//import javafx.scene.layout.Priority
//import voodoo.gui.FlatEntryController
//import voodoo.gui.FlatModpackController
//import voodoo.gui.FlatEntryWrapper
//import tornadofx.*
//
///**
// * Created by nikky on 18/03/18.
// * @author Nikky
// * @version 1.0
// */
//class EntryDetailList : View("FlatEntryWrapper List") {
//    val modpackController: FlatModpackController by inject()
//    val controller: FlatEntryController by inject()
//    //var tableViewEditModel: TableViewEditModel<FlatEntryWrapper> by singleAssign()
//    override val root = borderpane {
//        hgrow = Priority.ALWAYS //important
////        top = buttonbar {
////            button("COMMIT").setOnAction {
////                tableViewEditModel.commit()
////            }
////            button("ROLLBACK").setOnAction {
////                tableViewEditModel.rollback()
////            }
////        }
//        center = tableview(modpackController.selectedModpack.entries) {
//
//            column("name", FlatEntryWrapper::name).contentWidth(useAsMax = true)//.makeEditable()
//            column("CurseId", FlatEntryWrapper::id)
//            column("provider", FlatEntryWrapper::provider)//.useChoiceBox(Provider.values().toList().observable())
//            column("side", FlatEntryWrapper::side)//.useChoiceBox(Side.values().toList().observable())
//            column("description", FlatEntryWrapper::description)//.makeEditable()
//            column("comment", FlatEntryWrapper::comment)//.makeEditable()
//
//            bindSelected(controller.selectedEntry)
//
//            columnResizePolicy = SmartResize.POLICY
//
////            enableCellEditing() //enables easier cell navigation/editing
////            enableDirtyTracking() //flags cells that are dirty
////
////            tableViewEditModel = editModel
//        }
//    }
//}
