//package moe.nikky.voodoo.view
//
//import javafx.scene.layout.Priority
//import voodoo.gui.EntryController
//import voodoo.gui.ModpackController
//import voodoo.gui.EntryWrapper
//import tornadofx.*
//
///**
// * Created by nikky on 18/03/18.
// * @author Nikky
// * @version 1.0
// */
//class EntryDetailList : View("EntryWrapper List") {
//    val modpackController: ModpackController by inject()
//    val controller: EntryController by inject()
//    //var tableViewEditModel: TableViewEditModel<EntryWrapper> by singleAssign()
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
//            column("name", EntryWrapper::name).contentWidth(useAsMax = true)//.makeEditable()
//            column("CurseId", EntryWrapper::id)
//            column("provider", EntryWrapper::provider)//.useChoiceBox(Provider.values().toList().observable())
//            column("side", EntryWrapper::side)//.useChoiceBox(Side.values().toList().observable())
//            column("description", EntryWrapper::description)//.makeEditable()
//            column("comment", EntryWrapper::comment)//.makeEditable()
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
